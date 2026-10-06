package com.aurora

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.*

class GameOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private var isOverlayVisible = false
    private var targetPackageName: String? = null

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val fakeSavedStateRegistryOwner = object : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun start() {
            savedStateController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun stop() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
    }

    private val fakeViewModelStoreOwner = object : ViewModelStoreOwner {
        private val store = ViewModelStore()
        override val viewModelStore: ViewModelStore get() = store
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, createNotification())
        fakeSavedStateRegistryOwner.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") {
            stopSelf()
            return START_NOT_STICKY
        }

        targetPackageName = intent?.getStringExtra("PACKAGE_NAME")
        
        if (targetPackageName != null && !isOverlayVisible) {
            showOverlay()
            startAppMonitoring()
        }

        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (isOverlayVisible) return

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(fakeSavedStateRegistryOwner)
            setViewTreeViewModelStoreOwner(fakeViewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(fakeSavedStateRegistryOwner)

            setContent {
                MaterialTheme {
                    GameCornerOverlay(
                        onClose = {
                            stopSelf()
                        },
                        onDrag = { dx, dy ->
                            val layoutParams = layoutParams as WindowManager.LayoutParams
                            layoutParams.x += dx.toInt()
                            layoutParams.y += dy.toInt()
                            windowManager.updateViewLayout(this, layoutParams)
                        }
                    )
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(composeView, layoutParams)
        isOverlayVisible = true
    }

    private fun startAppMonitoring() {
        serviceScope.launch(Dispatchers.IO) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            while (isActive) {
                val currentApp = getForegroundApp(usageStatsManager)
                withContext(Dispatchers.Main) {
                    if (currentApp == targetPackageName) {
                        composeView.visibility = android.view.View.VISIBLE
                    } else if (currentApp != null && currentApp != packageName) {
                        // Hilang jika bukan game yang sedang aktif (dan bukan aplikasi aurora sendiri)
                        composeView.visibility = android.view.View.GONE
                    }
                }
                delay(1000)
            }
        }
    }

    private fun getForegroundApp(usageStatsManager: UsageStatsManager): String? {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // Look back 10 seconds
        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentForegroundPackage: String? = null
        
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentForegroundPackage = event.packageName
            }
        }
        return currentForegroundPackage
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        if (isOverlayVisible) {
            windowManager.removeView(composeView)
            isOverlayVisible = false
        }
        fakeSavedStateRegistryOwner.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "game_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Game Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, GameOverlayService::class.java).apply {
            action = "STOP_SERVICE"
        }
        val pendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aurora Game Corner")
            .setContentText("Game overlay is active")
            .setSmallIcon(R.drawable.ic_game)
            .addAction(R.drawable.ic_stop, "Stop", pendingIntent)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}

@Composable
fun GameCornerOverlay(
    onClose: () -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .wrapContentSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        if (!isExpanded) {
            // FLOATING ICON BUTTON (Collapsed State)
            Surface(
                color = Color(0xFF281813),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF8A65)),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable { isExpanded = true }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_game),
                        contentDescription = "Open Game Corner",
                        tint = Color(0xFFFFAB91),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            // FULL DASHBOARD WIDGET (Expanded State) - Meniru Axeron UI
            Surface(
                color = Color(0xEE160E0B),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xAAFF8A65)),
                modifier = Modifier.width(360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AURORA GAME CORNER",
                            color = Color(0xFFFF8A65),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Hardware Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(icon = Icons.Default.Speed, label = "FPS", value = "60")
                        StatItem(icon = Icons.Default.Memory, label = "RAM", value = "45%")
                        StatItem(icon = Icons.Default.Thermostat, label = "TEMP", value = "38°C")
                        StatItem(icon = Icons.Default.Wifi, label = "PING", value = "24ms")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Quick Actions
                    Text(
                        text = "DYNAMIC TWEAKS",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionButton(Icons.Default.Refresh, "Refresh Rate")
                        ActionButton(Icons.Default.Dns, "Tuner DNS")
                        ActionButton(Icons.Default.AdsClick, "Crosshair")
                        ActionButton(Icons.Default.RocketLaunch, "Boost")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF641E16)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tutup Overlay", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color(0xFFFFAB91), modifier = Modifier.size(20.dp))
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        Text(text = label, color = Color.Gray, fontSize = 10.sp)
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = Color(0xFF2B1813),
            shape = CircleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66FF8A65)),
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color(0xFFFFAB91), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.LightGray, fontSize = 10.sp)
    }
}
