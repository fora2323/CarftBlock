package com.aurora

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
        // Cek kembali status izin setiap kali activity kembali aktif (setelah dari Settings)
        checkRequiredPermissions()
    }

    private fun setupArchitectureText() {
        val tvArchitecture = findViewById<TextView>(R.id.tv_architecture)
    
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: ""
        val archBit = if (abi.contains("64")) "arm64-v8a" else "armeabi-v7a"
        tvArchitecture.text = archBit

        // 1. Simpan margin bawah asli dari XML agar tidak berlipat ganda
        val initialBottomMargin = (tvArchitecture.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin

        // 2. Terapkan WindowInsets untuk mendeteksi tinggi Navigation Bar
        ViewCompat.setOnApplyWindowInsetsListener(tvArchitecture) { view, windowInsets ->
            // Ambil ukuran inset khusus untuk sistem bar (navigasi bawah)
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            
            // Update layout params dengan margin asli + tinggi navigasi
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialBottomMargin + insets.bottom
            }
            
            // Kembalikan insets agar tetap bisa dipakai oleh view lain (jika ada)
            windowInsets
        }
    }

    private fun checkRequiredPermissions() {
        // Tutup dialog sebelumnya agar tidak menumpuk
        permissionDialog?.dismiss()

        when {
            // 1. Cek izin Notifikasi terlebih dahulu
            !SettingsUtility.isNotificationEnabled(this) -> {
                showPermissionDialog(
                    title = "Izin Notifikasi",
                    message = "Aplikasi ini memerlukan izin notifikasi untuk memberikan pembaruan penting. Silakan izinkan di pengaturan.",
                    onGrant = {
                        SettingsUtility.openNotificationSettings(this)
                    }
                )
            }
            // 2. Jika notifikasi diizinkan, cek Write System Settings (Abaikan izin Overlay)
            !SettingsUtility.canWriteSettings(this) -> {
                showPermissionDialog(
                    title = "Izin Modifikasi Sistem",
                    message = "Aplikasi memerlukan izin untuk mengubah pengaturan sistem agar dapat berfungsi maksimal.",
                    onGrant = {
                        SettingsUtility.openWriteSettings(this)
                    }
                )
            }
            // 3. Semua izin sudah diberikan
            else -> {
                proceedToMain()
            }
        }
    }

    private fun showPermissionDialog(title: String, message: String, onGrant: () -> Unit) {
        permissionDialog = AuroraDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            // .setIcon(R.drawable.ic_warning) // Hapus komentar jika Anda punya icon khusus
            .setPositiveButton(getString(R.string.title_button_dialog_allow_splash)) { dialog ->
                onGrant()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.title_button_dialog_close_splash)) { dialog ->
                dialog.dismiss()
                finish() // Keluar dari aplikasi jika izin ditolak
            }
            .build()
        
        permissionDialog?.setCancelable(false) // Memaksa user untuk merespon dialog
        permissionDialog?.show()
    }

    private fun proceedToMain() {
        // Lanjut ke Activity utama di sini
        // startActivity(Intent(this, MainActivity::class.java))
        // finish()
    }
}