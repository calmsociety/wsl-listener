package com.wsl.notifyhook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class BootReceiver : BroadcastReceiver() {

    private val TAG = "WSLBoot"

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == "android.intent.action.QUICKBOOT_POWERON") {

            val prefs = Prefs(context)
            val status = if (prefs.listenerEnabled) "✅ Listener aktif" else "❌ Listener mati"
            val kode = if (prefs.accessCode.isNotEmpty()) "🔑 accessCode OK" else "⚠️ accessCode kosong"

            Log.i(TAG, "📦 Boot selesai → $status, $kode")
            Toast.makeText(context, "WSL Listener siap! $status", Toast.LENGTH_LONG).show()

            // 🚀 Auto-start NotificationListener kalau toggle ON
            if (prefs.listenerEnabled) {
                try {
                    val svc = Intent(context, NotificationListener::class.java)
                    context.startService(svc)
                    Log.i(TAG, "🚀 NotificationListener dimulai ulang otomatis")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Gagal start NotificationListener: ${e.message}")
                }
            }
        }
    }
}
