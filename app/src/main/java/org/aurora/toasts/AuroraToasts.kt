package org.aurora // Sesuaikan dengan package-mu jika ditaruh di dalam folder lain

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ToastUI(message: String, isVisible: Boolean) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF2C2C2C), // Background Toast
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info, 
                    contentDescription = "Info",
                    tint = Color(0xFF4CAF50) // Warna icon hijau
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message, 
                    color = Color.White, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

fun Activity.showComposeToast(message: String, durationMs: Long = 2500) {
    val rootView = findViewById<ViewGroup>(android.R.id.content)
    
    val composeView = ComposeView(this)
    
    val layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        bottomMargin = 150 
    }

    composeView.setContent {
        var isVisible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            isVisible = true
        }

        ToastUI(message = message, isVisible = isVisible)
        
        LaunchedEffect(Unit) {
            delay(durationMs - 300) 
            isVisible = false
        }
    }

    rootView.addView(composeView, layoutParams)

    Handler(Looper.getMainLooper()).postDelayed({
        rootView.removeView(composeView)
    }, durationMs)
}