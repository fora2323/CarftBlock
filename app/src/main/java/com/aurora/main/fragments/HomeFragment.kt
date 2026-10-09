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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
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
                uptime = "Offline"
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

    // Style teks tanpa extra padding font supaya jarak antar teks bisa presisi
    val trimStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )

    Column(
        modifier = Modifier.fillMaxSize() 
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(brush = cardGradient)
        ) {
            // Logo watermark: dibungkus matchParentSize supaya ukuran logo
            // TIDAK ikut menambah tinggi card (penyebab gap di bawah overlay)
            Box(modifier = Modifier.matchParentSize()) {
                Image(
                    painter = painterResource(id = R.drawable.ic_dex_space),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.1f,
                    modifier = Modifier
                        .requiredSize(220.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = 60.dp, y = 10.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {

                // --- Bagian Paruh Atas ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "AURORA",
                        color = colorResource(id = R.color.outline),
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            letterSpacing = 1.sp,
                            lineHeightStyle = trimStyle
                        ),
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.tag_title_dex_space),
                        color = colorResource(id = R.color.onSurface),
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            lineHeightStyle = trimStyle
                        ),
                        modifier = Modifier.padding(top = 0.dp, bottom = 2.dp)
                    )
                    Text(
                        text = versionText,
                        color = colorResource(id = R.color.outline),
                        style = TextStyle(
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            lineHeightStyle = trimStyle
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // --- Bagian Paruh Bawah ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Overlay semi-transparan, menempel ke dasar card
                        .background(colorResource(id = R.color.surfaceContainerLowest).copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Widget Uptime dan PID
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Col Uptime
                        Column {
                            Text(
                                text = "UPTIME",
                                color = colorResource(id = R.color.outline),
                                fontWeight = FontWeight.SemiBold,
                                style = TextStyle(fontSize = 12.sp, lineHeight = 14.sp, lineHeightStyle = trimStyle),
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uptime,
                                color = colorResource(id = R.color.onSurface),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Col PID
                        Column {
                            Text(
                                text = "PID",
                                color = colorResource(id = R.color.outline),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pid,
                                color = colorResource(id = R.color.onSurface),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tombol "Launch"
                    Button(
                        onClick = onLaunchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.primaryContainer),
                            contentColor = colorResource(id = R.color.primary)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Launch",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
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