package com.wsl.notifyhook

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("notifyhook_prefs", Context.MODE_PRIVATE)

    var webhookUrl: String
        get() = sp.getString("webhook_url", "") ?: ""
        set(v) {
            sp.edit().putString("webhook_url", v).apply()
            println("✅ Set webhookUrl=$v")
        }

    var secret: String
        get() = sp.getString("secret", "") ?: ""
        set(v) {
            sp.edit().putString("secret", v).apply()
            println("✅ Set secret (len=${v.length})")
        }

    // ✅ simpan pilihan package user
    var selectedPackages: Set<String>
        get() = sp.getStringSet("selected_pkgs", emptySet()) ?: emptySet()
        set(v) {
            sp.edit().putStringSet("selected_pkgs", v).apply()
            println("✅ Saved ${v.size} selected packages")
        }

    // ✅ flag internal untuk toggle listener
    var listenerEnabled: Boolean
        get() = sp.getBoolean("listener_enabled", true)
        set(v) {
            sp.edit().putBoolean("listener_enabled", v).apply()
            println("✅ Listener enabled = $v")
        }

    // 🎯 Helper: true kalau pkg ada di whitelist (atau kalau kosong = semua lolos)
    fun isWhitelisted(pkg: String): Boolean {
        val s = selectedPackages
        if (s.isEmpty()) return false
        return pkg in s
    }
}
