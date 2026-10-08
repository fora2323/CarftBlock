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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
            // Compose Strategy agar memory tidak bocor saat Fragment hancur
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
    // State untuk menyimpan nilai Uptime dan PID
    var uptime by remember { mutableStateOf("---:--:--") }
    var pid by remember { mutableStateOf("---") }

    // Coroutine khusus Compose, otomatis mati jika fragment di background/dihancurkan
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
            
            // Tunggu 1 detik untuk perulangan berikutnya
            delay(1000)
        }
    }

    // --- STRUKTUR UI (Menduplikasi Gambar Desain) ---
    Box(
        modifier = Modifier
            .padding(14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF3E2E27)) // Warna background Cokelat Gelap
    ) {
        // Logo Watermark
        Image(
            painter = painterResource(id = R.drawable.ic_dex_space),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.15f,
            modifier = Modifier
                .size(180.dp)
                .offset(x = 30.dp)
                .align(Alignment.CenterEnd)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            
            // --- Bagian Paruh Atas ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 30.dp)
            ) {
                Text(
                    text = "AURORA",
                    color = Color(0xFFBCAEA4),
                    fontSize = 12.sp,
                    letterSpacing = 0.05.sp
                )
                Text(
                    text = stringResource(id = R.string.tag_title_dex_space),
                    color = Color(0xFFF1E8E2),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "V0.0.1-BETA",
                    color = Color(0xFFBCAEA4),
                    fontSize = 12.sp
                )
            }

            // --- Bagian Paruh Bawah (Lebih gelap) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF251C18)) // Warna bottom bar lebih gelap
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Widget Uptime dan PID
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Col Uptime
                    Column {
                        Text(
                            text = stringResource(id = R.string.tag_title_uptime),
                            color = Color(0xFFBCAEA4),
                            fontSize = 11.sp
                        )
                        Text(
                            text = uptime,
                            color = Color(0xFFF1E8E2),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.05.sp
                        )
                    }
                    
                    // Col PID
                    Column {
                        Text(
                            text = stringResource(id = R.string.tag_title_pid),
                            color = Color(0xFFBCAEA4),
                            fontSize = 11.sp
                        )
                        Text(
                            text = pid,
                            color = Color(0xFFF1E8E2),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.05.sp
                        )
                    }
                }

                // Tombol "Space" milikmu
                Button(
                    onClick = onLaunchClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF383838),
                        contentColor = Color(0xFF9AA0A6)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launch),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.tag_launch_dex_space),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}