package com.aurora

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope

// Import tambahan untuk Lottie & Coroutine
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog
import org.aurora.SettingsUtility
import com.aurora.main.activities.MainActivity
import com.aurora.starter.AuroraStarter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class SplashActivity : BaseCompatActivity() {

    private var permissionDialog: AuroraDialog? = null
    private var isCheckingFlow = false 

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)
        setupArchitectureText()
    }

    override fun onResume() {
        super.onResume()
        if (!isCheckingFlow) {
            startSplashFlow()
        }
    }

    private fun startSplashFlow() {
        isCheckingFlow = true
        
        lifecycleScope.launch {
            // 1. Eksekusi pengecekan Daemon secara ASYNC (berjalan di background bersamaan dengan animasi)
            val daemonCheckDeferred = async { checkDaemonViaSocket() }

            // 2. Tahan coroutine sampai animasi Lottie selesai berputar
            waitForLottieAnimation()

            // 3. Setelah animasi selesai, cek izin (biar dialog tidak menimpa animasi awal)
            val hasAllPermissions = checkRequiredPermissions()
            if (!hasAllPermissions) {
                isCheckingFlow = false
                return@launch 
            }

            // 4. Ambil hasil dari pengecekan daemon yang berjalan di langkah 1
            val isDaemonRunning = daemonCheckDeferred.await()

            // 5. Lakukan Routing Activity
            if (isDaemonRunning) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, ConnectedActivity::class.java))
            }
            
            finish()
        }
    }

    // Fungsi canggih untuk menahan proses Kotlin sampai animasi Lottie selesai
    private suspend fun waitForLottieAnimation() = suspendCancellableCoroutine<Unit> { cont ->
        val lottieView = findViewById<LottieAnimationView>(R.id.splash_icon)
        
        lottieView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                lottieView.removeAllAnimatorListeners()
                if (cont.isActive) cont.resume(Unit)
            }
            override fun onAnimationCancel(animation: Animator) {
                lottieView.removeAllAnimatorListeners()
                if (cont.isActive) cont.resume(Unit)
            }
        })
        
        // Mulai mainkan animasi
        lottieView.playAnimation()
    }

    private fun checkRequiredPermissions(): Boolean {
        permissionDialog?.dismiss()
        
        return when {
            !SettingsUtility.isNotificationEnabled(this) -> {
                showPermissionDialog(
                    title = "Izin Notifikasi",
                    message = "Aplikasi ini memerlukan izin notifikasi untuk memberikan pembaruan penting.",
                    onGrant = { SettingsUtility.openNotificationSettings(this) }
                )
                false
            }
            !SettingsUtility.canWriteSettings(this) -> {
                showPermissionDialog(
                    title = "Izin Modifikasi Sistem",
                    message = "Aplikasi memerlukan izin untuk mengubah pengaturan sistem agar dapat berfungsi maksimal.",
                    onGrant = { SettingsUtility.openWriteSettings(this) }
                )
                false
            }
            else -> true
        }
    }

    private fun showPermissionDialog(title: String, message: String, onGrant: () -> Unit) {
        permissionDialog = AuroraDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(getString(R.string.title_button_dialog_allow_splash)) { dialog ->
                onGrant()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.title_button_dialog_close_splash)) { dialog ->
                dialog.dismiss()
                finish()
            }
            .build()
            
        permissionDialog?.setCancelable(false)
        permissionDialog?.show()
    }

    private suspend fun checkDaemonViaSocket(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Socket("127.0.0.1", 52847).use { socket ->
                    socket.soTimeout = 1500
                    
                    val out = PrintWriter(socket.getOutputStream(), true)
                    val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                    
                    out.println("PING")
                    val response = `in`.readLine()
                    
                    response != null && response.startsWith("PONG")
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun setupArchitectureText() {
        val tvArchitecture = findViewById<TextView>(R.id.tv_architecture)
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        val archBit = if (abi.contains("64")) getString(R.string.title_abi64_splash)
                      else getString(R.string.title_abi32_splash)
        tvArchitecture.text = archBit

        val initialBottomMargin = (tvArchitecture.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(tvArchitecture) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottomMargin + insets.bottom
            }
            windowInsets
        }
    }
}