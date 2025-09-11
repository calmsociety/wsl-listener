package com.wsl.notifyhook

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class PersistentService : Service() {

    private val CHANNEL_ID = "WSL_LISTENER_CHANNEL"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // ✅ pake icon khusus notif (bukan ic_launcher adaptive)
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WSL Listener")
            .setContentText("✅ Listener aktif di background")
            .setSmallIcon(R.drawable.ic_stat_notify) // FIXED
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notif)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY → kalau service ke-kill, otomatis dihidupkan lagi
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WSL Listener Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
