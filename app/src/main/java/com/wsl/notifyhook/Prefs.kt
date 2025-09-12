package com.wsl.notifyhook

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("notifyhook_prefs", Context.MODE_PRIVATE)

    var accessCode: String
        get() = sp.getString("access_code", "") ?: ""
        set(v) {
            sp.edit().putString("access_code", v).apply()
            println("✅ Set accessCode (len=${v.length})")
        }

    var selectedPackages: Set<String>
        get() = sp.getStringSet("selected_pkgs", emptySet()) ?: emptySet()
        set(v) {
            sp.edit().putStringSet("selected_pkgs", v).apply()
            println("✅ Saved ${v.size} selected packages")
        }

    var listenerEnabled: Boolean
        get() = sp.getBoolean("listener_enabled", true)
        set(v) {
            sp.edit().putBoolean("listener_enabled", v).apply()
            println("✅ Listener enabled = $v")
        }

    // 📝 simpan log notifikasi terakhir (sudah dengan timestamp)
    var lastLog: String
        get() = sp.getString("last_log", "📭 Belum ada notifikasi") ?: "📭 Belum ada notifikasi"
        set(v) {
            sp.edit().putString("last_log", v).apply()
            println("📝 LastLog updated: $v")
        }

    fun isWhitelisted(pkg: String): Boolean {
        val s = selectedPackages
        if (s.isEmpty()) return false
        return pkg in s
    }
}
