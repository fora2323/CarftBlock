package com.aurora

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

        // ==========================================
        // LOGIKA TOMBOL PAIRING
        // ==========================================
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
                    // Pengecekan Kunci ADB: Hindari Pairing tidak sengaja jika sudah ada kunci
                    val keyStore = PreferenceAdbKeyStore(AuroraApplication.getAdbPrefs())
                    if (keyStore.get() != null) {
                        AuroraDialog.Builder(this)
                            .setTitle("Sudah Ter-Pairing?")
                            .setMessage("Aplikasi mendeteksi bahwa kunci rahasia ADB sudah tersimpan. Anda biasanya hanya perlu menekan tombol 'Start'.\n\nApakah Anda yakin ingin melakukan Pairing ulang? (Lakukan ini hanya jika 'Start' selalu gagal).")
                            .setPositiveButton("Ya, Pairing Ulang") { d ->
                                d.dismiss()
                                startPairingFlow()
                            }
                            .setNegativeButton("Batal") { d -> d.dismiss() }
                            .show()
                    } else {
                        startPairingFlow()
                    }
                }
            }
        }

        // ==========================================
        // LOGIKA TOMBOL START
        // ==========================================
        btnStart.setOnClickListener {
            val loading = AuroraLoading.newInstance()
            loading.showDialog(supportFragmentManager, Config.DIALOG_ADB_CONNECT_TAG)
            btnStart.isEnabled = false

            activityScope.launch {
                val result = runCatching {
                    var adbPort = -1
                    val portLatch = CountDownLatch(1)

                    // 1. Coba deteksi port via mDNS (Tunggu maksimal 3 detik)
                    val mdns = AdbMdns(this@ConnectedActivity, AdbMdns.TLS_CONNECT, Observer { port ->
                        if (port > 0 && adbPort == -1) {
                            adbPort = port
                            portLatch.countDown()
                        }
                    })
                    mdns.start()

                    val found = portLatch.await(3, TimeUnit.SECONDS)
                    mdns.stop()

                    // 2. Logika Fallback: Jika mDNS gagal, coba ketuk port TCP 5555
                    if (found && adbPort > 0) {
                        Log.d(TAG, "Step 1: Ditemukan port Wireless mDNS = $adbPort")
                    } else {
                        Log.d(TAG, "Step 1: mDNS gagal/timeout. Fallback ke TCP Port 5555...")
                        
                        val isPort5555Open = try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress("127.0.0.1", 5555), 1000)
                                true
                            }
                        } catch (e: Exception) {
                            false
                        }

                        if (isPort5555Open) {
                            adbPort = 5555
                            Log.d(TAG, "Step 1: Fallback sukses! Menggunakan port 5555.")
                        } else {
                            throw Exception("Tidak dapat menemukan port ADB. Pastikan Wireless Debugging aktif atau port TCP 5555 terbuka.")
                        }
                    }

                    // 3. Load Key & Connect
                    val key = AdbKey(PreferenceAdbKeyStore(AuroraApplication.getAdbPrefs()), "aurora")
                    val client = AdbClient("127.0.0.1", adbPort, key)
                    client.connect()
                    
                    // 4. Jalankan Daemon dengan nohup agar kebal terhadap putusnya sesi ADB
                    val command = AuroraStarter.getInternalCommand(this@ConnectedActivity)
                    Log.d(TAG, "Step 5: Eksekusi = $command")
                    client.shellCommand("nohup sh -c \"$command\" > /dev/null 2>&1 &") { output ->
                        Log.d(TAG, "Daemon Output: " + String(output))
                    }
                    client.close()
                    Log.d(TAG, "Step 6: Shell selesai — daemon launched")
                }

                // 5. Update UI di Main Thread
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

    // ==========================================
    // FUNGSI PEMBANTU (HELPERS)
    // ==========================================
    private fun startPairingFlow() {
        try {
            startForegroundService(AdbPairingService.startIntent(this))
        } catch (e: Exception) {
            Log.e(TAG, "Gagal start AdbPairingService", e)
            AuroraDialog.Builder(this)
                .setTitle("Gagal Memulai Layanan")
                .setMessage(e.message ?: "Error tidak diketahui")
                .setPositiveButton("OK") { d -> d.dismiss() }
                .show()
            return // Hentikan eksekusi jika service gagal dijalankan
        }

        try {
            val intent = Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                // Fallback Settings Intent jika custom UI manufaktur berbeda
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "Tidak bisa buka Developer Options", ex)
            }
        }
    }
}