package com.aurora.main.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.aurora.GameLauncherActivity
import com.aurora.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.home_fragment, container, false)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    HomeContent(
                        onLaunchClick = {
                            if (context != null) {
                                val intent = Intent(requireContext(), GameLauncherActivity::class.java)
                                startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
        
        return view
    }
}

@Composable
fun HomeContent(onLaunchClick: () -> Unit) {
    var uptime by remember { mutableStateOf("---:--:--") }
    var pid by remember { mutableStateOf("---") }

    LaunchedEffect(Unit) {
        while (isActive) {
            var isRunning = false
            var daemonPid = "---"
            var daemonStartTime = 0L

            withContext(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress("127.0.0.1", 52847), 500)
                        socket.soTimeout = 500
                        
                        val out = PrintWriter(socket.getOutputStream(), true)
                        val inReader = BufferedReader(InputStreamReader(socket.getInputStream()))

                        out.println("PING")
                        val response = inReader.readLine()

                        if (response != null && response.startsWith("PONG:")) {
                            isRunning = true
                            val parts = response.split(":")
                            if (parts.size >= 3) {
                                daemonPid = parts[1]
                                daemonStartTime = parts[2].toLong()
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }

            if (isRunning) {
                var millis = System.currentTimeMillis() - daemonStartTime
                if (millis < 0) millis = 0
                
                val seconds = (millis / 1000) % 60
                val minutes = (millis / (1000 * 60)) % 60
                val hours = (millis / (1000 * 60 * 60))

                uptime = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                pid = daemonPid
            } else {
                uptime = "___"
                pid = "---"
            }
            
            delay(1000)
        }
    }

    // Gradasi horizontal: kiri primaryContainer -> kanan surfaceContainer
    val cardGradient = Brush.horizontalGradient(
        colors = listOf(
            colorResource(id = R.color.primaryContainer),
            colorResource(id = R.color.surfaceContainer)
        )
    )

    // Versi diambil dari versionName asli aplikasi
    val context = LocalContext.current
    val versionText = remember {
        @Suppress("DEPRECATION")
        val name = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
        "V${name ?: "-"}".uppercase(Locale.US)
    }

    val trimStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Proportional,
        trim = LineHeightStyle.Trim.Both
    )
    // Buang padding bawaan font (penyebab utama jarak antar teks masih jauh)
    @Suppress("DEPRECATION")
    val noFontPadding = PlatformTextStyle(includeFontPadding = false)
    val cardFont = FontFamily(Font(R.font.force))
    val cardFontCondensed = FontFamily(Font(R.font.condensed))

    Column(
        modifier = Modifier.fillMaxSize() 
    ) {
        Box(
            modifier = Modifier
                // PADDING LUAR CARD: horizontal = kiri-kanan, vertical = atas-bawah
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                // Radius sudut card
                .clip(RoundedCornerShape(16.dp))
                // Background gradasi (lihat cardGradient di atas)
                .background(brush = cardGradient)
        ) {
            // Logo watermark (dibungkus matchParentSize supaya tidak menambah tinggi card)
            Box(modifier = Modifier.matchParentSize()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dex_space),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.1f,
                    colorFilter = ColorFilter.tint(colorResource(R.color.onPrimaryContainer)),
                    modifier = Modifier
                        .requiredSize(150.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-8).dp, y = 0.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {

                // ===== BAGIAN ATAS: AURORA / judul / versi =====
                // Pakai Box (bukan Column): tiap teks diposisikan sendiri lewat padding(top),
                // jadi jaraknya tidak tergantung tinggi baris font.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // PADDING DALAM BAGIAN ATAS (top/bottom dinaikkan = card makin tinggi)
                        .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 18.dp)
                ) {
                    Text(
                        text = "AURORA",
                        overflow = TextOverflow.Visible,
                        color = colorResource(id = R.color.outline),
                        fontFamily = cardFontCondensed, // font condensed
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 12.sp,
                            lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                        ),
                        // POSISI AURORA dari atas (0 = paling atas)
                        modifier = Modifier.padding(top = 0.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.tag_title_dex_space),
                        overflow = TextOverflow.Visible,
                        color = colorResource(id = R.color.onSurface),
                        fontFamily = cardFont, // font force
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 28.sp,
                            lineHeight = 28.sp,    // samakan dengan fontSize
                            lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                        ),
                        // POSISI JUDUL dari atas: kecilkan = makin dekat ke AURORA
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = versionText,
                        overflow = TextOverflow.Visible,
                        color = colorResource(id = R.color.outline),
                        fontFamily = cardFont, // font force
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 11.sp,    // samakan dengan fontSize
                            lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                        ),
                        // POSISI VERSI dari atas: kira-kira posisi judul + 28 (kalau judul digeser, geser juga ini)
                        modifier = Modifier.padding(top = 40.dp)
                    )
                }

                // ===== BAGIAN BAWAH: uptime, PID, tombol Launch =====
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Warna + alpha overlay bawah (0.55f, ubah sesuai selera)
                        .background(colorResource(id = R.color.surfaceContainerLowest).copy(alpha = 0.55f))
                        // PADDING overlay bawah: horizontal = kiri-kanan, vertical = atas-bawah
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        // JARAK antara kolom UPTIME dan PID
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // --- Kolom UPTIME ---
                        Column {
                            Text(
                                text = "UPTIME",
                                color = colorResource(id = R.color.outline),
                                fontFamily = cardFont, // font force (ganti di val cardFont)
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    fontSize = 12.sp,    // ukuran label
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.5.sp,
                                    lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                                )
                            )
                            // Margin top nilai uptime (0.dp = rapat ke label)
                            Text(
                                text = uptime,
                                color = colorResource(id = R.color.onSurface),
                                fontFamily = cardFont, // font force (ganti di val cardFont)
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(
                                    fontSize = 16.sp,    // ukuran nilai
                                    lineHeight = 20.sp,
                                    lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                                ),
                                modifier = Modifier.padding(top = 0.dp)
                            )
                        }

                        // --- Kolom PID (dibuat sama persis dengan UPTIME) ---
                        Column {
                            Text(
                                text = "PID",
                                color = colorResource(id = R.color.outline),
                                fontFamily = cardFont, // font force (ganti di val cardFont)
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 14.sp,
                                    letterSpacing = 0.5.sp,
                                    lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                                )
                            )
                            Text(
                                text = pid,
                                color = colorResource(id = R.color.onSurface),
                                fontFamily = cardFont, // font force (ganti di val cardFont)
                                fontWeight = FontWeight.Bold,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    lineHeightStyle = trimStyle,
                            platformStyle = noFontPadding
                                ),
                                modifier = Modifier.padding(top = 0.dp)
                            )
                        }
                    }

                    // --- Tombol Launch ---
                    Button(
                        onClick = onLaunchClick,
                        colors = ButtonDefaults.buttonColors(
                            // warna background tombol
                            containerColor = colorResource(id = R.color.primaryContainer),
                            // warna teks + icon tombol
                            contentColor = colorResource(id = R.color.primary)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        // PADDING tombol kiri-kanan (horizontal) dan atas-bawah (vertical)
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 0.dp),
                        // tinggi tombol
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Launch",
                            fontFamily = cardFont, // font force (ganti di val cardFont)
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // jarak teks ke icon
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launch),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}