package com.aurora

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatActivity

class AuroraApplication : Application() {

    companion object {
        private lateinit var mApplicationContext: Context

        fun getContext(): Context = mApplicationContext
    }

    override fun onCreate() {
        super.onCreate()
        mApplicationContext = applicationContext
    }
}