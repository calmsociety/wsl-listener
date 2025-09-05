package com.wsl.notifyhook

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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

            if (prefs.listenerEnabled) {
                try {
                    forceRebindNotificationListener(context)
                    Log.i(TAG, "🔄 NotificationListener direbind otomatis")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Gagal rebind NotificationListener: ${e.message}")
                }
            }
        }
    }

    private fun forceRebindNotificationListener(context: Context) {
        val cn = ComponentName(context, NotificationListener::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            cn,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            cn,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
