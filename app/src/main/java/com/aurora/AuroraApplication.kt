package com.aurora

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build

import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

import org.aurora.EmptySharedPreferencesImpl
import org.lsposed.hiddenapibypass.HiddenApiBypass

class AuroraApplication : Application() {

    companion object {
        private lateinit var mApplicationContext: Context
        private lateinit var mDeviceProtectedPrefs: SharedPreferences

        fun getContext(): Context = mApplicationContext
        fun getAdbPrefs(): SharedPreferences = mDeviceProtectedPrefs

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                HiddenApiBypass.setHiddenApiExemptions("")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        mApplicationContext = applicationContext
        mDeviceProtectedPrefs = getStorageContext().getSharedPreferences("aurora_adb_prefs", Context.MODE_PRIVATE)

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

    private fun getStorageContext(): Context {
        val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createDeviceProtectedStorageContext()
        else
            this

        // Fallback ke EmptySharedPreferencesImpl kalau storage belum unlock (sama seperti Shizuku)
        return object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                return try {
                    super.getSharedPreferences(name, mode)
                } catch (e: IllegalStateException) {
                    EmptySharedPreferencesImpl()
                }
            }
        }
    }
}