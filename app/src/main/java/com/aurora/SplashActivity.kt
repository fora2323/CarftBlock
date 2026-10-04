package com.aurora

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog
import org.aurora.SettingsUtility
import com.aurora.main.activities.MainActivity

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
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
            val daemonCheckDeferred = async { checkDaemonViaSocket() }

            delay(2000)

            val hasAllPermissions = checkRequiredPermissions()
            if (!hasAllPermissions) {
                isCheckingFlow = false
                return@launch 
            }

            val isDaemonRunning = daemonCheckDeferred.await()

            if (isDaemonRunning) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SplashActivity, ConnectedActivity::class.java))
            }
            
            finish()
        }
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
            // Retry hingga 3 kali dengan timeout yang lebih longgar (1.5s)
            for (i in 1..3) {
                try {
                    Socket().use { socket ->
                        socket.tcpNoDelay = true
                        socket.connect(InetSocketAddress("127.0.0.1", 52847), 1500)
                        socket.soTimeout = 1500
                        
                        val out = PrintWriter(socket.getOutputStream(), true)
                        val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                        
                        out.println("PING")
                        val response = `in`.readLine()
                        
                        if (response != null && response.contains("PONG")) {
                            return@withContext true
                        }
                    }
                } catch (e: Throwable) {
                    // Abaikan dan coba lagi
                }
                delay(300)
            }
            false
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