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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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

    // Gradasi background utama Card disamakan dengan warna XML
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            colorResource(id = R.color.surfaceContainerHighest),
            colorResource(id = R.color.surfaceContainer)
        ),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Column(
        modifier = Modifier.fillMaxSize() 
    ) {
        Box(
            modifier = Modifier
                // Padding luar Card: Kiri-Kanan 14dp, Atas-Bawah 4dp (sesuai instruksi)
                .padding(horizontal = 14.dp, vertical = 4.dp) 
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                // Menghilangkan .shadow() (elevation) sesuai instruksi
                .background(brush = cardGradient)
        ) {
            // Logo Watermark
            Image(
                painter = painterResource(id = R.drawable.ic_dex_space),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = 0.1f, // Alpha diredupkan sedikit agar flat
                modifier = Modifier
                    .size(220.dp)
                    .offset(x = 60.dp, y = 10.dp)
                    .align(Alignment.CenterEnd)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                
                // --- Bagian Paruh Atas ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = "AURORA",
                        color = colorResource(id = R.color.outline),
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        // Padding bottom 0 (sesuai instruksi)
                        modifier = Modifier.padding(bottom = 0.dp)
                    )
                    // Spacer dihapus agar AURORA dan Dex Space rapat
                    Text(
                        text = stringResource(id = R.string.tag_title_dex_space),
                        color = colorResource(id = R.color.onSurface),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        // Padding top 0 (sesuai instruksi)
                        modifier = Modifier.padding(top = 0.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "V0.0.1-BETA",
                        color = colorResource(id = R.color.outline),
                        fontSize = 11.sp
                    )
                }

                // --- Bagian Paruh Bawah ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Menggunakan warna paling gelap dari XML (menyerupai screenshot)
                        .background(colorResource(id = R.color.surfaceContainerLowest)) 
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Widget Uptime dan PID
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        // Col Uptime
                        Column {
                            Text(
                                text = "UPTIME",
                                color = colorResource(id = R.color.outline),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uptime,
                                color = colorResource(id = R.color.onSurface),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        // Col PID
                        Column {
                            Text(
                                text = "PID",
                                color = colorResource(id = R.color.outline),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pid,
                                color = colorResource(id = R.color.onSurface),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Tombol "Launch"
                    Button(
                        onClick = onLaunchClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorResource(id = R.color.color_white_slowest),
                            contentColor = colorResource(id = R.color.color_white_high)
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