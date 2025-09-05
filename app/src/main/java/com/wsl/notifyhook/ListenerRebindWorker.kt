package com.wsl.notifyhook

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.work.Worker
import androidx.work.WorkerParameters

class ListenerRebindWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val TAG = "WSLRebind"

    override fun doWork(): Result {
        val prefs = Prefs(applicationContext)

        if (prefs.listenerEnabled) {
            val isActive = isListenerSystemEnabled(applicationContext)
            if (!isActive) {
                Log.w(TAG, "⚠️ Listener hilang → force rebind")
                forceRebindNotificationListener(applicationContext)
                showToast("🔄 Listener direbind otomatis")
            } else {
                Log.d(TAG, "✅ Listener masih aktif")
            }
        } else {
            Log.d(TAG, "⏸ Listener dimatikan user → skip")
        }

        return Result.success()
    }

    private fun isListenerSystemEnabled(context: Context): Boolean {
        val cn = ComponentName(context, NotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { it.equals(cn.flattenToString(), ignoreCase = true) }
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

    private fun showToast(msg: String) {
        android.os.Handler(applicationContext.mainLooper).post {
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
