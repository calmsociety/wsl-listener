package com.wsl.notifyhook.utils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import com.wsl.notifyhook.NotificationListener
import com.wsl.notifyhook.Prefs

fun ensureNotificationAccess(context: Context, prefs: Prefs, tvStatus: TextView) {
    if (!isListenerSystemEnabled(context)) {
        Toast.makeText(context, "⚠️ Izinkan akses notifikasi agar NotifyHook berfungsi", Toast.LENGTH_LONG).show()
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
    tvStatus.text = statusText(context, prefs)
}

fun isListenerSystemEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
    return flat.split(":").any { it.equals(cn.flattenToString(), ignoreCase = true) }
}

fun ensureBatteryOptimization(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
        Toast.makeText(context, "⚠️ Matikan optimasi baterai agar service tidak mati", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    }
}

fun statusText(context: Context, prefs: Prefs): String {
    val sys = isListenerSystemEnabled(context)
    return when {
        !sys -> "⚠️ Akses sistem belum diizinkan"
        prefs.listenerEnabled -> "✅ Listener: ON"
        else -> "❌ Listener: OFF"
    }
}

fun formatAppList(prefs: Prefs): String {
    val pkgs = prefs.selectedPackages
    return if (pkgs.isEmpty()) {
        "📭 Tidak ada aplikasi dipilih."
    } else {
        "📱 Aplikasi yang di-listen:\n- " + pkgs.joinToString("\n- ")
    }
}
