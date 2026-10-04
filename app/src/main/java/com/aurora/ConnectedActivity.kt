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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog
import org.aurora.dialogs.AuroraLoading
import org.aurora.SettingsUtility
import org.aurora.Config
import com.aurora.toasts.showComposeToast // Sesuaikan dengan nama packagenya


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
        val btnStep: MaterialButton = findViewById(R.id.btn_step)
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
        
        btnStep.setOnClickListener {
            showComposeToast("Berhasil login!")
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

        btnStart.setOnClickListener {
            val loading = AuroraLoading.newInstance()
            loading.showDialog(supportFragmentManager, Config.DIALOG_ADB_CONNECT_TAG)
            btnStart.isEnabled = false

            activityScope.launch {
                val result = runCatching {
                    var adbPort = -1
                    val portLatch = CountDownLatch(1)

                    val mdns = AdbMdns(this@ConnectedActivity, AdbMdns.TLS_CONNECT, Observer { port ->
                        if (port > 0 && adbPort == -1) {
                            adbPort = port
                            portLatch.countDown()
                        }
                    })
                    mdns.start()

                    val found = portLatch.await(3, TimeUnit.SECONDS)
                    mdns.stop()

                    if (found && adbPort > 0) {
                        Log.d(TAG, "Step 1: Ditemukan port Wireless mDNS = $adbPort")
                    } else {
                        Log.d(TAG, "Step 1: mDNS gagal/timeout. Fallback ke TCP Port 5555...")
                        val isPort5555Open = try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress("127.0.0.1", 5555), 1000)
                                true
                            }
                        } catch (e: Exception) { false }

                        if (isPort5555Open) {
                            adbPort = 5555
                        } else {
                            throw Exception("Tidak dapat menemukan port ADB. Pastikan Wireless Debugging aktif atau port TCP 5555 terbuka.")
                        }
                    }

                    // Load Key & Connect
                    val key = AdbKey(PreferenceAdbKeyStore(AuroraApplication.getAdbPrefs()), "aurora")
                    val client = AdbClient("127.0.0.1", adbPort, key)
                    client.connect()
                    
                    // ==========================================
                    // PERBAIKAN: EKSEKUSI AMAN & DETACH SEMPURNA
                    // ==========================================
                    val command = AuroraStarter.getInternalCommand(this@ConnectedActivity)
                    
                    // Menjalankan langsung dari lokasi aslinya. 
                    // Perintah diarahkan ke /dev/null dan ditaruh di background (&) agar terlepas dari jalur ADB.
                    val detachCommand = "sh -c \"$command >/dev/null 2>&1 &\""
                    
                    Log.d(TAG, "Step 2: Menjalankan Eksekusi -> $detachCommand")
                    client.shellCommand(detachCommand) { _ -> 
                        // Output sengaja kosong karena stream sudah dialihkan ke /dev/null
                    }
                    client.close()

                    // ==========================================
                    // PERBAIKAN: VERIFIKASI DAEMON
                    // ==========================================
                    Log.d(TAG, "Step 3: Memverifikasi apakah daemon berhasil hidup...")
                    var isDaemonReady = false
                    
                    for (i in 1..10) {
                        delay(500) // Tunggu 0.5 detik setiap percobaan
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress("127.0.0.1", 52847), 500)
                                socket.soTimeout = 500
                                val out = PrintWriter(socket.getOutputStream(), true)
                                val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                                
                                out.println("PING")
                                val response = `in`.readLine()
                                if (response != null && response.startsWith("PONG")) {
                                    isDaemonReady = true
                                    return@use
                                }
                            }
                        } catch (e: Exception) {
                            // Abaikan error, loop akan mencoba lagi
                        }
                        if (isDaemonReady) break
                    }

                    if (!isDaemonReady) {
                        throw Exception("Daemon terhubung tetapi langsung mati. Pastikan libaurora.so kompatibel dengan perangkat ini.")
                    }
                    Log.d(TAG, "Step 4: Daemon terverifikasi hidup!")
                }

                withContext(Dispatchers.Main) {
                    loading.dismissDialog()
                    btnStart.isEnabled = true

                    if (result.isSuccess) {
                        Log.i(TAG, "Berhasil masuk ke MainActivity!")
                        startActivity(Intent(this@ConnectedActivity, MainActivity::class.java))
                        finish()
                    } else {
                        val err = result.exceptionOrNull()
                        Log.e(TAG, "Gagal: ${err?.message}", err)
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
            return
        }

        try {
            val intent = Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra(":settings:fragment_args_key", "toggle_adb_wireless")
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } catch (ex: Exception) {
                Log.e(TAG, "Tidak bisa buka Developer Options", ex)
            }
        }
    }
}