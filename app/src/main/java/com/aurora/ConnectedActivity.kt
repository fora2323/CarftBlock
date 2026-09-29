package com.aurora

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.appcompat.widget.Toolbar

import com.google.android.material.button.MaterialButton

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog
import org.aurora.SettingsUtility

import com.aurora.adb.AdbClient
import com.aurora.adb.AdbKey
import com.aurora.adb.AdbPairingService
import com.aurora.adb.PreferenceAdbKeyStore

class ConnectedActivity : BaseCompatActivity() {

    companion object {
        private const val TAG = "ConnectedActivity"
        private const val RESULT_CHANNEL = "aurora_result_channel"
        private const val RESULT_NOTIF_ID = 100
    }

    private val activityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connected)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val rootView: View = findViewById(android.R.id.content)
        val btn2: MaterialButton = findViewById(R.id.btn_pairing)
        val btn3: MaterialButton = findViewById(R.id.btn_start)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        createResultNotificationChannel()

        // ── PAIRING BUTTON ───────────────────────────────────────────
        btn2.setOnClickListener {
            if (!SettingsUtility.isNotificationEnabled(this)) {
                // Minta izin notifikasi dulu
                AuroraDialog.Builder(this)
                    .setTitle("Izin Notifikasi Diperlukan")
                    .setMessage("Izin notifikasi wajib diaktifkan agar kode pairing 6-digit bisa muncul di notifikasi.")
                    .setPositiveButton("Izinkan") { dialog ->
                        SettingsUtility.openNotificationSettings(this)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Batal") { dialog -> dialog.dismiss() }
                    .show()
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                AuroraDialog.Builder(this)
                    .setTitle("Tidak Didukung")
                    .setMessage("ADB Wireless Pairing membutuhkan Android 11 atau lebih baru.")
                    .setPositiveButton("OK") { d -> d.dismiss() }
                    .show()
            } else {
                // Izin OK + Android 11+ → start service
                val intent = AdbPairingService.startIntent(this)
                try {
                    startForegroundService(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal start AdbPairingService", e)
                    AuroraDialog.Builder(this)
                        .setTitle("Gagal Memulai Layanan")
                        .setMessage(e.message ?: "Error tidak diketahui")
                        .setPositiveButton("OK") { d -> d.dismiss() }
                        .show()
                }
            }
        }

        // ── START ENGINE BUTTON ──────────────────────────────────────
        btn3.setOnClickListener {
            btn3.isEnabled = false
            btn3.text = "Menghubungkan..."

            activityScope.launch {
                val result = runCatching {
                    val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        createDeviceProtectedStorageContext()
                    else
                        this@ConnectedActivity

                    val prefs = storageContext.getSharedPreferences("aurora_adb_prefs", Context.MODE_PRIVATE)
                    val key = AdbKey(PreferenceAdbKeyStore(prefs), "aurora")

                    AdbClient("127.0.0.1", 5555, key).also { it.connect() }
                }

                withContext(Dispatchers.Main) {
                    btn3.isEnabled = true
                    btn3.text = "Start Engine"

                    if (result.isSuccess) {
                        Log.i(TAG, "ADB connect success")
                        showResultNotification(
                            title = "Aurora Engine Terhubung ✓",
                            text = "Koneksi ADB ke 127.0.0.1:5555 berhasil. Aurora siap digunakan."
                        )
                        result.getOrNull()?.close()
                    } else {
                        val err = result.exceptionOrNull()
                        Log.e(TAG, "ADB connect failed", err)
                        showResultNotification(
                            title = "Koneksi Gagal ✗",
                            text = "Tidak dapat terhubung ke ADB: ${err?.message ?: err.toString()}"
                        )
                    }
                }
            }
        }

        applySystemBarInsets(rootView, toolbar, null)
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private fun createResultNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(RESULT_CHANNEL, "Aurora Connection Result", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { setSound(null, null); setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun showResultNotification(title: String, text: String) {
        if (!SettingsUtility.isNotificationEnabled(this)) return
        val notif = Notification.Builder(this, RESULT_CHANNEL)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(RESULT_NOTIF_ID, notif)
    }
}