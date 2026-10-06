package org.aurora.toasts

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ToastUI(message: String, isVisible: Boolean, durationMs: Long = 2500L) {
    val progressAnim = remember { Animatable(1f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            progressAnim.snapTo(1f)
            progressAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = durationMs.toInt(),
                    easing = LinearEasing
                )
            )
        }
    }

    val outlineColor = colorResource(id = com.aurora.R.color.primary)
    val backgroundColor = colorResource(id = com.aurora.R.color.surfaceContainerHigh)
    val iconColor = colorResource(id = com.aurora.R.color.primary)
    val textColor = colorResource(id = com.aurora.R.color.onSurface)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .drawWithContent {
                    drawContent()
                    val cornerRadiusPx = 12.dp.toPx()
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                0f, 0f, size.width, size.height,
                                CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }
                    val pathMeasure = PathMeasure()
                    pathMeasure.setPath(path, forceClosed = false)
                    
                    val destPath = Path()
                    pathMeasure.getSegment(
                        startDistance = 0f,
                        stopDistance = pathMeasure.length * progressAnim.value,
                        destination = destPath,
                        startWithMoveTo = true
                    )
                    
                    drawPath(
                        path = destPath,
                        color = outlineColor,
                        style = Stroke(width = 2.dp.toPx())
                    )
                },
            shape = RoundedCornerShape(12.dp),
            color = backgroundColor,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info, 
                    contentDescription = "Info",
                    tint = iconColor 
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message, 
                    color = textColor, 
                    fontSize = 14.sp
                )
            }
        }
    }
}

class AuroraToast private constructor(
    private val activity: Activity,
    private val message: String,
    private val durationMs: Long
) {
    companion object {
        const val LENGTH_SHORT = 2500L
        const val LENGTH_LONG = 4000L
    }

    class Builder(private val activity: Activity) {
        private var message: String = ""
        private var durationMs: Long = LENGTH_SHORT

        fun setMessage(message: String) = apply { this.message = message }
        fun setDuration(duration: Long) = apply { this.durationMs = duration }

        fun build(): AuroraToast = AuroraToast(activity, message, durationMs)

        fun show() {
            build().show()
        }
    }

    fun show() {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = ComposeView(activity)
        
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
                delay(durationMs)
                isVisible = false
            }

            ToastUI(message = message, isVisible = isVisible, durationMs = durationMs)
        }

        rootView.addView(composeView, layoutParams)

        // Berikan waktu ekstra agar animasi fadeOut selesai sebelum view dihapus
        Handler(Looper.getMainLooper()).postDelayed({
            rootView.removeView(composeView)
        }, durationMs + 300L)
    }
}

@Preview
@Composable
private fun ToastUIPreview() {
    ToastUI(
        message = "This is a sample toast message",
        isVisible = true,
        durationMs = 2500L
    )
}