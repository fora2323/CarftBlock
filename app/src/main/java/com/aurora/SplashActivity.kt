package com.aurora

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog
import org.aurora.SettingsUtility

import com.aurora.main.activities.MainActivity
import com.aurora.starter.AuroraStarter

class SplashActivity : BaseCompatActivity() {

    private var permissionDialog: AuroraDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash)

        setupArchitectureText()
        checkRequiredPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkRequiredPermissions()
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

    private fun checkRequiredPermissions() {
        permissionDialog?.dismiss()
        when {
            !SettingsUtility.isNotificationEnabled(this) -> {
                showPermissionDialog(
                    title = "Izin Notifikasi",
                    message = "Aplikasi ini memerlukan izin notifikasi untuk memberikan pembaruan penting.",
                    onGrant = { SettingsUtility.openNotificationSettings(this) }
                )
            }
            !SettingsUtility.canWriteSettings(this) -> {
                showPermissionDialog(
                    title = "Izin Modifikasi Sistem",
                    message = "Aplikasi memerlukan izin untuk mengubah pengaturan sistem agar dapat berfungsi maksimal.",
                    onGrant = { SettingsUtility.openWriteSettings(this) }
                )
            }
            else -> proceedToMain()
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

    private fun proceedToMain() {
        val isDaemonRunning = try {
            AuroraStarter.getInternalCommand(this) // binary ada
            isDaemonActive()
        } catch (e: Exception) {
            false
        }

        if (isDaemonRunning) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            startActivity(Intent(this, ConnectedActivity::class.java))
        }
        finish()
    }

    private fun isDaemonActive(): Boolean {
        return try {
            val result = Runtime.getRuntime().exec("pgrep -f libaurora.so").inputStream.bufferedReader().readLine()
            !result.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }
}