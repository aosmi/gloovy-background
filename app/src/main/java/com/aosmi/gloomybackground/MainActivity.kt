package com.aosmi.gloomybackground

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val prefs get() = getSharedPreferences("overlay", 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val seek = findViewById<SeekBar>(R.id.seekBar)
        val status = findViewById<TextView>(R.id.tvStatus)
        val start = findViewById<Button>(R.id.btnStart)
        val stop = findViewById<Button>(R.id.btnStop)

        seek.progress = prefs.getInt("alpha", 30)
        status.text = "Затемнение: ${seek.progress}%"

        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                status.text = "Затемнение: $progress%"
                if (fromUser && OverlayService.isRunning && Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java).apply {
                        action = OverlayService.ACTION_START
                        putExtra(OverlayService.EXTRA_ALPHA, progress)
                    })
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {
                prefs.edit().putInt("alpha", seek.progress).apply()
            }
        })

        start.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Разрешите показ поверх других приложений", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            startService(Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START
                putExtra(OverlayService.EXTRA_ALPHA, seek.progress)
            })
            start.isEnabled = false
            stop.isEnabled = true
            seek.postDelayed({ refreshUi() }, 500)
        }

        stop.setOnClickListener {
            startService(Intent(this, OverlayService::class.java).apply { action = OverlayService.ACTION_STOP })
            start.isEnabled = true
            stop.isEnabled = false
            seek.postDelayed({ refreshUi() }, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun refreshUi() {
        val active = OverlayService.isRunning && Settings.canDrawOverlays(this)
        findViewById<Button>(R.id.btnStart).isEnabled = !active
        findViewById<Button>(R.id.btnStop).isEnabled = active
    }
}