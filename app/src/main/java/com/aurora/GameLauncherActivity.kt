package com.aurora

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.aurora.core.BaseCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

data class InstalledGameModel(
    val id: String,
    val name: String,
    val packageName: String,
    val iconDrawable: Drawable?
)

class GameLauncherActivity : BaseCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        setContent {
            MaterialTheme {
                GameLauncherScreen(
                    onBackClicked = { finish() }
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameLauncherScreen(
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    // Colors from colors.xml
    val primaryColor = colorResource(id = R.color.primary)
    val primaryContainerColor = colorResource(id = R.color.primaryContainer)
    val onSurfaceColor = colorResource(id = R.color.onSurface)

    // Query installed games dynamically from PackageManager
    var games by remember { mutableStateOf<List<InstalledGameModel>>(emptyList()) }
    var isLoadingGames by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = queryInstalledGames(context)
            withContext(Dispatchers.Main) {
                games = list
                isLoadingGames = false
            }
        }
    }

    var selectedGame by remember { mutableStateOf<InstalledGameModel?>(null) }

    // Dynamic Time & Battery
    var currentTime by remember { mutableStateOf("") }
    var batteryLevel by remember { mutableIntStateOf(100) }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

        while (true) {
            currentTime = sdf.format(Date())
            batteryLevel = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            delay(1000.milliseconds)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ==========================================
        // 1. HONEYCOMB / HEXAGON GRID BACKGROUND PATTERN
        // ==========================================
        HoneycombBackground(
            selectedGame = selectedGame,
            modifier = Modifier.fillMaxSize()
        )

        // ==========================================
        // 2. MAIN CONTENT LAYOUT (Paddings Konsisten)
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // TOP HEADER BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "DEX SPACE",
                        color = onSurfaceColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = currentTime,
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(primaryContainerColor)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$batteryLevel%",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Header Action: Unselect / Reset Game Selection
                if (selectedGame != null) {
                    TextButton(onClick = { selectedGame = null }) {
                        Text(
                            text = "Tutup Detail",
                            color = primaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MIDDLE SECTION: Game List (Kiri) & Detail Panel (Kanan)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // SISI KIRI: DAFTAR GAME
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                ) {
                    if (isLoadingGames) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = primaryColor)
                        }
                    } else if (games.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tidak ada game terdeteksi",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(games) { game ->
                                val isSelected = selectedGame?.id == game.id
                                InstalledGameItemCard(
                                    game = game,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedGame = game
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(28.dp))

                // SISI KANAN: DETAIL & TOMBOL START GAME (Tepat Di Atas Tombol Start)
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (selectedGame != null) {
                            // JUDUL GAME TEPAT DI ATAS TOMBOL START GAME (DENGAN MARQUEE JIKA PANJANG)
                            Text(
                                text = selectedGame!!.name,
                                color = onSurfaceColor,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .wrapContentWidth(Alignment.End)
                                    .basicMarquee(iterations = Int.MAX_VALUE)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // TOMBOL START GAME
                            Surface(
                                color = primaryContainerColor,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, primaryColor),
                                modifier = Modifier
                                    .clickable {
                                        val pm = context.packageManager
                                        val launchIntent = pm.getLaunchIntentForPackage(selectedGame!!.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(context, "Gagal membuka ${selectedGame!!.name}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "Start Game",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 36.dp, vertical = 14.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Pilih game untuk memulai",
                                color = Color(0xAAFFFFFF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // TOMBOL AKSI BAWAH
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionIconButton(icon = Icons.AutoMirrored.Filled.ExitToApp) {
                                onBackClicked()
                            }
                            ActionIconButton(icon = Icons.Default.Gamepad) {
                                Toast.makeText(context, "Mode Game Controller", Toast.LENGTH_SHORT).show()
                            }
                            ActionIconButton(icon = Icons.Default.Settings) {
                                Toast.makeText(context, "Pengaturan Game Space", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // FOOTER / STATUS BAR DI BAWAH
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = colorResource(id = R.color.surfaceContainer),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colorResource(id = R.color.outlineVariant))
                ) {
                    Text(
                        text = "Masih versi beta, jika ada masalah teknis silahkan hubungi developer",
                        color = colorResource(id = R.color.onSurfaceVariant),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * HONEYCOMB / HEXAGON GRID PATTERN CANVAS BACKGROUND
 * Warna diambil dari resources colors.xml
 */
@Composable
fun HoneycombBackground(
    selectedGame: InstalledGameModel?,
    modifier: Modifier = Modifier
) {
    val bgColor = colorResource(id = R.color.background)
    val surfaceContainerColor = colorResource(id = R.color.surfaceContainer)
    val primaryContainerColor = colorResource(id = R.color.primaryContainer)
    val gridLineColor = colorResource(id = R.color.outline).copy(alpha = 0.20f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Background Radial Gradient menggunakan warna dari colors.xml
        val baseGradient = if (selectedGame != null) {
            Brush.radialGradient(
                colors = listOf(
                    primaryContainerColor.copy(alpha = 0.6f),
                    surfaceContainerColor,
                    bgColor
                ),
                center = Offset(width * 0.75f, height * 0.45f),
                radius = width * 0.85f
            )
        } else {
            Brush.radialGradient(
                colors = listOf(
                    surfaceContainerColor,
                    bgColor,
                    bgColor
                ),
                center = Offset(width * 0.50f, height * 0.50f),
                radius = width * 0.85f
            )
        }
        drawRect(brush = baseGradient)

        // Draw Honeycomb Hexagon Grid Pattern
        val hexRadius = 26.dp.toPx()
        val hexWidth = sqrt(3f) * hexRadius
        val hexHeight = 2f * hexRadius
        val strokeWidth = 1.2.dp.toPx()

        val cols = (width / hexWidth).toInt() + 3
        val rows = (height / (hexHeight * 0.75f)).toInt() + 3

        for (row in -1..rows) {
            val y = row * hexHeight * 0.75f
            val xOffset = if (row % 2 != 0) hexWidth / 2f else 0f

            for (col in -1..cols) {
                val x = col * hexWidth + xOffset
                val path = Path()

                for (i in 0..5) {
                    val angleRad = (60 * i + 30) * Math.PI / 180f
                    val px = x + hexRadius * cos(angleRad).toFloat()
                    val py = y + hexRadius * sin(angleRad).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()

                drawPath(
                    path = path,
                    color = gridLineColor,
                    style = Stroke(width = strokeWidth)
                )
            }
        }
    }
}

/**
 * DESAIN KARTU DAFTAR GAME MENGGUNAKAN WARNA colors.xml
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InstalledGameItemCard(
    game: InstalledGameModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = colorResource(id = R.color.primary)
    val primaryContainerColor = colorResource(id = R.color.primaryContainer)
    val surfaceContainerColor = colorResource(id = R.color.surfaceContainer)
    val surfaceContainerHighColor = colorResource(id = R.color.surfaceContainerHigh)

    val bgBrush = if (isSelected) {
        Brush.horizontalGradient(
            colors = listOf(primaryContainerColor, surfaceContainerHighColor)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(surfaceContainerColor, surfaceContainerColor)
        )
    }

    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.2.dp, primaryColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgBrush)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconBitmap = remember(game.iconDrawable) {
                game.iconDrawable?.toBitmap(96, 96)?.asImageBitmap()
            }

            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = game.name,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, if (isSelected) primaryColor else Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(primaryColor)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // TITLE GAME DENGAN MARQUEE PADA KARTU (Jika nama game panjang)
            Text(
                text = game.name,
                color = colorResource(id = R.color.onSurface),
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
        }
    }
}

/**
 * Mengambil daftar game terpasang dari perangkat pengguna
 */
private fun queryInstalledGames(context: Context): List<InstalledGameModel> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
    val result = mutableListOf<InstalledGameModel>()

    for (info in resolveInfos) {
        val pkgName = info.activityInfo.packageName
        if (pkgName == context.packageName) continue

        val appInfo = info.activityInfo.applicationInfo
        val isGame =
            appInfo.category == ApplicationInfo.CATEGORY_GAME

        if (isGame) {
            val appName = info.loadLabel(pm).toString()
            val appIcon = info.loadIcon(pm)
            result.add(
                InstalledGameModel(
                    id = pkgName,
                    name = appName,
                    packageName = pkgName,
                    iconDrawable = appIcon
                )
            )
        }
    }

    // Jika tidak ada game terdeteksi via kategori, ambil aplikasi buatan pengguna
    if (result.isEmpty()) {
        for (info in resolveInfos) {
            val pkgName = info.activityInfo.packageName
            val appInfo = info.activityInfo.applicationInfo

            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && pkgName != context.packageName) {
                val appName = info.loadLabel(pm).toString()
                val appIcon = info.loadIcon(pm)
                result.add(
                    InstalledGameModel(
                        id = pkgName,
                        name = appName,
                        packageName = pkgName,
                        iconDrawable = appIcon
                    )
                )
            }
        }
    }

    return result
}

@Composable
fun ActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val primaryColor = colorResource(id = R.color.primary)
    val primaryContainerColor = colorResource(id = R.color.primaryContainer)
    val surfaceContainerHighColor = colorResource(id = R.color.surfaceContainerHigh)

    Surface(
        color = surfaceContainerHighColor,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, primaryContainerColor),
        modifier = Modifier
            .size(42.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
