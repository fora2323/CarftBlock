package com.aurora

import android.os.Bundle
import android.view.View

import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar

import com.google.android.material.button.MaterialButton

import org.aurora.core.BaseCompatActivity
import org.aurora.dialogs.AuroraDialog

class ConnectedActivity : BaseCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.connected)
        
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val rootView: View = findViewById(android.R.id.content)
        val btn1: MaterialButton = findViewById(R.id.btn_instruksi) 
        val btn2: MaterialButton = findViewById(R.id.btn_pairing) 
        val btn3: MaterialButton = findViewById(R.id.btn_start) 
        
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btn2.setOnClickListener {
            AuroraDialog.Builder(this)
                .setTitle("Izin Notifikasi Diperlukan")
                .setMessage("Izin notifikasi wajib diaktifkan agar aplikasi dapat menampilkan status sistem, informasi penting, dan informasi saat aplikasi berjalan atau ditutup.")
                .setPositiveButton("Izinkan") { dialog ->
                    dialog.dismiss() 
                }
                .setNegativeButton("Batal") { dialog ->
                    dialog.dismiss()
                }
                .show()
        }

        applySystemBarInsets(rootView, toolbar, null)
    }
}