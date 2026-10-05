package com.aurora.adb

import android.annotation.TargetApi
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log

import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import java.net.ConnectException

import com.aurora.AuroraApplication
import com.aurora.R

@TargetApi(Build.VERSION_CODES.R)
class AdbPairingService : Service() {

    companion object {
        const val notificationChannel = "adb_pairing"
        private const val tag = "AdbPairingService"
        
        private const val notificationId = 1
        private const val replyRequestId = 1
        private const val stopRequestId = 2
        private const val retryRequestId = 3
        
        private const val startAction = "start"
        private const val stopAction = "stop"
        private const val replyAction = "reply"
        
        private const val remoteInputResultKey = "paring_code"
        private const val portKey = "paring_port"

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

    // Timer batas waktu pairing (Anti-ANR Android 14)
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        Log.w(tag, "Pairing timeout. Terminating service to prevent ANR.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannel, 
                getString(R.string.notification_channel_adb_pairing), 
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(false)
                }
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timeoutHandler.postDelayed(timeoutRunnable, 150000)

        val notification = when (intent?.action) {
            startAction -> { 
                onStart() 
            }
            replyAction -> {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                // Menggunakan RemoteInput dari AndroidX
                val remoteInputResults = RemoteInput.getResultsFromIntent(intent)
                val code = remoteInputResults?.getCharSequence(remoteInputResultKey)?.toString() ?: ""
                val port = intent.getIntExtra(portKey, -1)
                
                if (port != -1 && code.isNotEmpty()) {
                    onInput(code, port)
                } else {
                    onStart()
                }
            }
            stopAction -> {
                timeoutHandler.removeCallbacks(timeoutRunnable)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                null
            }
            else -> { 
                return START_NOT_STICKY
            }
        }

        if (notification != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
                // Mengambil shared preference menggunakan AuroraApplication sesuai kodemu
                @Suppress("UNUSED_VARIABLE")
                val prefs = storageContext.getSharedPreferences("aurora_adb_prefs", Context.MODE_PRIVATE)
                AdbKey(PreferenceAdbKeyStore(AuroraApplication.getAdbPrefs()), "aurora")
            } catch (e: Throwable) {
                handleResult(false, e)
                return@launch
            }

            AdbPairingClient(host, port, code, key).runCatching {
                start()
            }.onFailure {
                handleResult(false, it)
            }.onSuccess { success ->
                handleResult(success, null)
            }
        }
        return workingNotification
    }

    private fun handleResult(success: Boolean, exception: Throwable?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        val title: String
        val text: String?

        if (success) {
            title = getString(R.string.notification_adb_pairing_succeed_title)
            text = getString(R.string.notification_adb_pairing_succeed_text)
            stopSearch()
        } else {
            title = getString(R.string.notification_adb_pairing_failed_title)
            text = when (exception) {
                is ConnectException -> getString(R.string.cannot_connect_port)
                is AdbInvalidPairingCodeException -> getString(R.string.paring_code_is_wrong)
                is AdbKeyException -> getString(R.string.adb_error_key_store)
                else -> exception?.message ?: exception?.toString() ?: "Unknown Error"
            }
        }

        val builder = NotificationCompat.Builder(this, notificationChannel)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_aurora)
            .setAutoCancel(true)

        if (!success) {
            builder.addAction(retryNotificationAction)
        }

        getSystemService(NotificationManager::class.java).notify(notificationId, builder.build())
        stopSelf()
    }

    // ── NOTIFICATION ACTIONS (MENGGUNAKAN ANDROIDX) ────────────────────

    private val stopNotificationAction by lazy {
        val pendingIntent = PendingIntent.getService(
            this, stopRequestId, stopIntent(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )
        NotificationCompat.Action.Builder(
            0, 
            getString(R.string.notification_adb_pairing_stop_searching), 
            pendingIntent
        ).build()
    }

    private val retryNotificationAction by lazy {
        val pendingIntent = PendingIntent.getService(
            this, retryRequestId, startIntent(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0
        )
        NotificationCompat.Action.Builder(
            0, 
            getString(R.string.notification_adb_pairing_retry), 
            pendingIntent
        ).build()
    }

    private fun replyNotificationAction(port: Int): NotificationCompat.Action {
        val remoteInput = RemoteInput.Builder(remoteInputResultKey).run {
            setLabel(getString(R.string.dialog_adb_pairing_paring_code))
            build()
        }

        val pendingIntent = PendingIntent.getForegroundService(
            this, replyRequestId + port, replyIntent(this, port), 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        return NotificationCompat.Action.Builder(
            0, 
            getString(R.string.notification_adb_pairing_input_paring_code), 
            pendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    // ── NOTIFICATION BUILDERS ──────────────────────────────────────────

    private val searchingNotification by lazy {
        NotificationCompat.Builder(this, notificationChannel)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(getString(R.string.notification_adb_pairing_searching_for_service_title))
            .setSmallIcon(R.drawable.ic_aurora)
            .setContentIntent(getSettingsPendingIntent()) 
            .addAction(stopNotificationAction)
            .build()
    }

    private val workingNotification by lazy {
        NotificationCompat.Builder(this, notificationChannel)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(getString(R.string.notification_adb_pairing_working_title))
            .setSmallIcon(R.drawable.ic_aurora)
            .build()
    }

    private fun createInputNotification(port: Int): Notification {
        return NotificationCompat.Builder(this, notificationChannel)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setContentTitle(getString(R.string.notification_adb_pairing_service_found_title))
            .setSmallIcon(R.drawable.ic_aurora)
            .addAction(replyNotificationAction(port)) 
            .build()
    }

    // ── UTILITIES ──────────────────────────────────────────────────────

    private fun getSettingsPendingIntent(): PendingIntent {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val highlightKey = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "toggle_adb_wireless" else "enable_adb" 
            putExtra(":settings:fragment_args_key", highlightKey)
            putExtra(":settings:show_fragment_args", Bundle().apply { putString(":settings:fragment_args_key", highlightKey) })
        }
        
        return PendingIntent.getActivity(
            this, 3, intent, 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null
}