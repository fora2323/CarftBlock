package com.aurora

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.button.MaterialButton
import org.aurora.core.BaseCompatActivity

class DebugActivity : BaseCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var scrollView: NestedScrollView
    private lateinit var tvLog: TextView
    private lateinit var btnCopy: MaterialButton
    private lateinit var btnRestart: MaterialButton
    private lateinit var bottomContainer: View

    private var fullLog: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug)

        toolbar = findViewById(R.id.toolbar)
        scrollView = findViewById(R.id.scroll_view)
        tvLog = findViewById(R.id.tv_log)
        btnCopy = findViewById(R.id.btn_copy)
        btnRestart = findViewById(R.id.btn_restart)
        bottomContainer = findViewById(R.id.bottom_container)

        val initialBottomPadding = bottomContainer.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.setPadding(toolbar.paddingLeft, systemBars.top, toolbar.paddingRight, toolbar.paddingBottom)
            bottomContainer.setPadding(bottomContainer.paddingLeft, bottomContainer.paddingTop, bottomContainer.paddingRight, initialBottomPadding + systemBars.bottom)
            insets
        }

        fullLog = intent.getStringExtra("EXTRA_CRASH_LOG") ?: "No error log available"
        tvLog.text = fullLog

        val colorSurface = ContextCompat.getColor(this, R.color.surface)
        val colorSurfaceContainer = ContextCompat.getColor(this, R.color.surfaceContainer)

        scrollView.setOnScrollChangeListener { _: NestedScrollView, _: Int, scrollY: Int, _: Int, _: Int ->
            if (scrollY > 0) {
                toolbar.setBackgroundColor(colorSurfaceContainer)
            } else {
                toolbar.setBackgroundColor(colorSurface)
            }
        }

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