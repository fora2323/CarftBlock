package com.aurora

import android.content.Intent
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
import org.aurora.dialogs.AuroraLoading
import org.aurora.SettingsUtility
import org.aurora.Config

import com.aurora.adb.AdbPairingService
import com.aurora.adb.AdbMdns
import com.aurora.adb.AdbKey
import com.aurora.adb.AdbClient
import com.aurora.adb.PreferenceAdbKeyStore
import com.aurora.main.activities.MainActivity
import com.aurora.starter.AuroraStarter

class ConnectedActivity : BaseCompatActivity() {

    companion object {
        private const val TAG = "ConnectedActivity"
    }

    private val activityScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connected)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val rootView: View = findViewById(android.R.id.content)
        val btnPairing: MaterialButton = findViewById(R.id.btn_pairing)
        val btnStart: MaterialButton = findViewById(R.id.btn_start)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnPairing.setOnClickListener {
            when {
                !SettingsUtility.isNotificationEnabled(this) -> {
                    AuroraDialog.Builder(this)
                        .setTitle("Izin Notifikasi Diperlukan")
                        .setMessage("Izin notifikasi wajib diaktifkan agar kode pairing 6-digit bisa muncul.")
                        .setPositiveButton("Izinkan") { dialog ->
                            SettingsUtility.openNotificationSettings(this)
                            dialog.dismiss()
                        }
                        .setNegativeButton("Batal") { dialog -> dialog.dismiss() }
                        .show()
                }
                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> {
                    AuroraDialog.Builder(this)
                        .setTitle("Tidak Didukung")
                        .setMessage("ADB Wireless Pairing membutuhkan Android 11 atau lebih baru.")
                        .setPositiveButton("OK") { d -> d.dismiss() }
                        .show()
                }
                else -> {
                    try {
                        startForegroundService(AdbPairingService.startIntent(this))
                    } catch (e: Exception) {
                        Log.e(TAG, "Gagal start AdbPairingService", e)
                        AuroraDialog.Builder(this)
                            .setTitle("Gagal Memulai Layanan")
                            .setMessage(e.message ?: "Error tidak diketahui")
                            .setPositiveButton("OK") { d -> d.dismiss() }
                            .show()
                    }

                    try {
                        val intent = Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Tidak bisa buka Developer Options", ex)
                        }
                    }
                }
            }
        }

        btnStart.setOnClickListener {
            val loading = AuroraLoading.newInstance()
            loading.showDialog(supportFragmentManager, Config.DIALOG_ADB_CONNECT_TAG)
            btnStart.isEnabled = false

            activityScope.launch {
                val result = runCatching {
                    var adbPort = -1
                    val portLatch = java.util.concurrent.CountDownLatch(1)

                    val mdns = AdbMdns(this@ConnectedActivity, AdbMdns.TLS_CONNECT,
                        androidx.lifecycle.Observer { port ->
                            if (port > 0 && adbPort == -1) {
                                adbPort = port
                                portLatch.countDown()
                            }
                        }
                    )
                    mdns.start()

                    val found = portLatch.await(10, java.util.concurrent.TimeUnit.SECONDS)
                    mdns.stop()

                    if (!found || adbPort <= 0) throw Exception("Tidak dapat menemukan port ADB wireless. Pastikan Wireless Debugging aktif.")
                    Log.d(TAG, "Step 2: ADB port = $adbPort")

                    // Step 2: Load key
                    val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        createDeviceProtectedStorageContext()
                    else
                        this@ConnectedActivity
                    val prefs = storageContext.getSharedPreferences("aurora_adb_prefs", android.content.Context.MODE_PRIVATE)
                    val key = AdbKey(PreferenceAdbKeyStore(AuroraApplication.getAdbPrefs()), "aurora")

                    val client = AdbClient("127.0.0.1", adbPort, key)
                    client.connect()
                    
                    // Step 4: Jalankan daemon via shell
                    val command = AuroraStarter.getInternalCommand(this@ConnectedActivity)
                    Log.d(TAG, "Step 5: Eksekusi = $command")
                    client.shellCommand(command, null)
                    client.close()
                    Log.d(TAG, "Step 6: Shell selesai — daemon launched")
                    Log.d(TAG, "Step 7: Done")
                }

                withContext(Dispatchers.Main) {
                    loading.dismissDialog()
                    btnStart.isEnabled = true

                    if (result.isSuccess) {
                        Log.i(TAG, "Daemon started successfully")
                        startActivity(Intent(this@ConnectedActivity, MainActivity::class.java))
                        finish()
                    } else {
                        val err = result.exceptionOrNull()
                        Log.e(TAG, "Daemon failed: ${err?.message}", err)
                        AuroraDialog.Builder(this@ConnectedActivity)
                            .setTitle("Gagal Menjalankan Engine")
                            .setMessage(err?.message ?: "Error tidak diketahui")
                            .setPositiveButton("OK") { d -> d.dismiss() }
                            .show()
                    }
                }
            }
        }

        applySystemBarInsets(rootView, toolbar, null)
    }
}