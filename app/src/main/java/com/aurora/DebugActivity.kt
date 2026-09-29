package com.aurora

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast

import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.appcompat.widget.toolbar

import com.google.android.material.button.MaterialButton

import org.aurora.core.BaseCompatActivity

class DebugActivity : BaseCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var scrollView: NestedScrollView
    private lateinit var tvLog: TextView
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnRestart: MaterialButton
    private var fullLog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug)

        toolbar = findViewById(R.id.toolbar)
        scrollView = findViewById(R.id.scroll_view)
        tvLog = findViewById(R.id.tv_log)
        btnCopy = findViewById(R.id.btn_copy)
        btnRestart = findViewById(R.id.btn_restart)

        fullLog = intent.getStringExtra("EXTRA_CRASH_LOG") ?: "No error log available"
        tvLog.text = fullLog

        val colorSurface = ContextCompat.getColor(this, R.color.surface)
        val colorSurfaceContainer = ContextCompat.getColor(this, R.color.surfaceContainer)

        scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
            if (scrollY > 0) {
                toolbar.setBackgroundColor(colorSurfaceContainer)
            } else {
                toolbar.setBackgroundColor(colorSurface)
            }
        })

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Crash Log", fullLog)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Log copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        btnRestart.setOnClickListener {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(launchIntent)
            }
            finish()
        }
    }
}