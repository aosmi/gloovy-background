package com.aosmi.gloomybackground

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager

class OverlayService : Service() {

    companion object {
        const val ACTION_START = "com.aosmi.gloomybackground.START"
        const val ACTION_STOP = "com.aosmi.gloomybackground.STOP"
        const val EXTRA_ALPHA = "alpha"

        @Volatile
        var isRunning = false
            private set
    }

    private var view: View? = null
    private lateinit var wm: WindowManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val ch = NotificationChannel("overlay", "Затемнение", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                if (view == null) startForeground(1, makeNotification())
                showOverlay(intent.getIntExtra(EXTRA_ALPHA, 30))
            }
            ACTION_STOP -> {
                view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
                view = null
                getSharedPreferences("overlay", 0).edit().putBoolean("active", false).apply()
                stopForeground(true)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        view = null
        isRunning = false
        getSharedPreferences("overlay", 0).edit().putBoolean("active", false).apply()
        super.onDestroy()
    }

    private fun showOverlay(percent: Int) {
        val a = (percent / 100f).coerceAtMost(0.8f)
        val v = view
        if (v != null) {
            val p = v.layoutParams as WindowManager.LayoutParams
            p.alpha = a
            wm.updateViewLayout(v, p)
            return
        }
        val overlay = View(this).apply { setBackgroundColor(Color.BLACK) }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT
        )
        params.alpha = a
        try {
            wm.addView(overlay, params)
            view = overlay
            getSharedPreferences("overlay", 0).edit().putBoolean("active", true).apply()
        } catch (e: Exception) {
            Log.e("Overlay", "addView failed", e)
            stopSelf()
        }
    }

    private fun makeNotification(): Notification {
        val stop = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val pi = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(this, "overlay")
            .setContentTitle("Затемнение включено")
            .setContentText("Нажмите чтобы выключить")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(0, "Выключить", pi)
            .build()
    }
}