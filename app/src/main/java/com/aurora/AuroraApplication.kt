package com.aurora

import android.app.Application
import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class AuroraApplication : Application() {

    companion object {
        private lateinit var mApplicationContext: Context

        fun getContext(): Context = mApplicationContext
    }

    override fun onCreate() {
        super.onCreate()
        mApplicationContext = applicationContext

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashLog = sw.toString()

            val intent = Intent(this, DebugActivity::class.java).apply {
                putExtra("EXTRA_CRASH_LOG", crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
            exitProcess(1)
        }
    }
}