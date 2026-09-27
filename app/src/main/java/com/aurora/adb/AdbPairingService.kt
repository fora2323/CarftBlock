package com.aurora.adb

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle // [PERBAIKAN ERROR] Import Bundle ditambahkan di sini
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.Observer
import com.cyrust.fftoolspromaxnew.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.net.ConnectException

@TargetApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {
        const val notificationChannel = "adb_pairing"
        private const val tag = "AdbPairingService"
        private const val notificationId = 1
        private const val replyRequestId = 1
        private const val stopRequestId = 2
        
        private const val startAction = "start"
        private const val stopAction = "stop"
        private const val replyAction = "reply"
        
        private const val remoteInputResultKey = "pairing_code"
        private const val portKey = "pairing_port"

        fun startIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(startAction)
        }

        private fun stopIntent(context: Context): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(stopAction)
        }

        private fun replyIntent(context: Context, port: Int): Intent {
            return Intent(context, AdbPairingService::class.java).setAction(replyAction).putExtra(portKey, port)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var adbMdns: AdbMdns? = null

    // [ANTI-ANR] Timer Handler to auto-terminate the service within 2.5 Minutes (150,000 ms)
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        Log.w(tag, "Pairing timeout (2.5 minutes). Terminating service to prevent Android 14 ANR.")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private val observer = Observer<Int> { port ->
        Log.i(tag, "Pairing service port detected: $port")
        if (port <= 0) return@Observer
        val notification = createInputNotification(port)
        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    private var started = false

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            notificationChannel,
            "Onyx ADB Pairing Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            setShowBadge(false)
            setAllowBubbles(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        // [ANTI-ANR] Reset & start 2.5 Minutes countdown when service is called
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 150000)

        val notification = when (intent?.action) {
            startAction -> { 
                onStart() 
            }
            replyAction -> {
                // Stop the timer if the user has entered the code
                timeoutHandler.removeCallbacks(timeoutRunnable)
                
                val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
                val code = remoteInputResults?.getCharSequence(remoteInputResultKey)?.toString() ?: ""
                val port = intent.getIntExtra(portKey, -1)
                
                Log.i(tag, "Received pairing code. Port: $port, Code length: ${code.length}")
                
                if (port != -1 && code.isNotEmpty()) {
                    onInput(code, port)
                } else {
                    onStart()
                }
            }
            stopAction -> {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                null
            }
            else -> { 
                onStart() 
            }
        }

        if (notification != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 14+, ensure dataSync type is declared in the manifest if required
                    startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST)
                } else {
                    startForeground(notificationId, notification)
                }
            } catch (e: Throwable) {
                Log.e(tag, "Error starting foreground service", e)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is ForegroundServiceStartNotAllowedException) {
                    getSystemService(NotificationManager::class.java).notify(notificationId, notification)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun startSearch() {
        if (started) return
        started = true
        adbMdns = AdbMdns(this, AdbMdns.TLS_PAIRING, observer).apply { start() }
    }

    private fun stopSearch() {
        if (!started) return
        started = false
        adbMdns?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSearch()
        // [ANTI-ANR] Clear the timer to prevent memory leaks
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    private fun onStart(): Notification {
        startSearch()
        return searchingNotification
    }

    private fun onInput(code: String, port: Int): Notification {
        serviceScope.launch {
            val host = "127.0.0.1"
            val key = try {
                val storageContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    createDeviceProtectedStorageContext()
                } else {
                    this@AdbPairingService 
                }
                
                val prefs = storageContext.getSharedPreferences("ffx_adb_prefs", Context.MODE_PRIVATE)
                AdbKey(PreferenceAdbKeyStore(prefs), "ffx")
                
            } catch (e: Throwable) {
                Log.e(tag, "Failed to load ADB key", e)
                handleResult(false, e)
                return@launch
            }

            AdbPairingClient(host, port, code, key).runCatching {
                start()
            }.onFailure {
                Log.e(tag, "Pairing failed", it)
                handleResult(false, it)
            }.onSuccess { success ->
                Log.i(tag, "Pairing success result: $success")
                handleResult(success, null)
            }
        }
        return workingNotification
    }

    private fun handleResult(success: Boolean, exception: Throwable?) {
        stopForeground(STOP_FOREGROUND_REMOVE)

        val title: String
        val text: String?

        if (success) {
            title = "Pairing Successful"
            text = "Device authorized. Please return to the app and tap START ENGINE."
            stopSearch()
        } else {
            title = "Pairing Failed"
            
            // [PERBAIKAN]: Menangkap error Conscrypt/TLS dan memberikan pesan yang jelas ke User
            val errMessage = exception?.message ?: exception?.toString() ?: ""
            
            text = when {
                exception is ConnectException -> "Unable to connect to the wireless debugging port."
                exception is AdbInvalidPairingCodeException -> "The 6-digit Pairing Code entered is incorrect."
                exception is AdbKeyException -> "Failed to generate RSA security key."
                errMessage.contains("Conscrypt") || errMessage.contains("exportKeyingMaterial") -> 
                    "Sistem keamanan HP memblokir TLS Pairing. Silakan kembali dan gunakan 'START VIA TCP (PORT 5555)'."
                else -> "Authorization Error: $errMessage"
            }
        }

        val notification = Notification.Builder(this, notificationChannel)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.app_icon) // Ganti jika nama icon Anda berbeda
            .setAutoCancel(true)
            .build()

        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
        stopSelf()
    }


    private val stopNotificationAction by lazy {
        val pendingIntent = PendingIntent.getService(
            this, stopRequestId, stopIntent(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )
        Notification.Action.Builder(null, "Stop Searching", pendingIntent).build()
    }

    private fun createInputNotification(port: Int): Notification {
        val remoteInput = RemoteInput.Builder(remoteInputResultKey).run {
            setLabel("Enter 6-Digit Pairing Code")
            build()
        }
        
        val intent = replyIntent(this, port)
        val pendingIntent = PendingIntent.getForegroundService(
            this, replyRequestId + port, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val replyAction = Notification.Action.Builder(null, "Enter Code", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        return Notification.Builder(this, notificationChannel)
            .setContentTitle("Onyx Pairing Service Detected")
            .setContentText("Expand this notification and tap 'Enter Code' to authenticate.")
            .setSmallIcon(R.drawable.app_icon)
            .addAction(replyAction)
            .addAction(stopNotificationAction)
            .build()
    }

    private val searchingNotification by lazy {
        Notification.Builder(this, notificationChannel)
            .setContentTitle("Searching for Pairing Service...")
            // Inform the user that this notification is clickable
            .setContentText("Tap here to enable 'Wireless Debugging' in Developer Options.")
            .setSmallIcon(R.drawable.app_icon)
            // [NEW FEATURE] Click the notification to open settings and highlight the menu!
            .setContentIntent(getSettingsPendingIntent()) 
            .addAction(stopNotificationAction)
            .build()
    }

    private val workingNotification by lazy {
        Notification.Builder(this, notificationChannel)
            .setContentTitle("Authenticating Onyx Daemon...")
            .setContentText("Establishing secure connection with Android system...")
            .setSmallIcon(R.drawable.app_icon)
            .build()
    }

    // =========================================================
    // SMART NOTIFICATION CLICK (Blinking Settings)
    // =========================================================
    private fun getSettingsPendingIntent(): PendingIntent {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Menerapkan kunci toggle_adb_wireless sesuai permintaan Anda
            val highlightKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "toggle_adb_wireless" 
            } else {
                "enable_adb" 
            }
            
            putExtra(":settings:fragment_args_key", highlightKey)
            
            val bundle = Bundle().apply { 
                putString(":settings:fragment_args_key", highlightKey) 
            }
            putExtra(":settings:show_fragment_args", bundle)
        }
        
        return PendingIntent.getActivity(
            this, 
            3, 
            intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
