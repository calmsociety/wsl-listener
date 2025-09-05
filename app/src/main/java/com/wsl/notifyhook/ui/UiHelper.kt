package com.wsl.notifyhook.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import com.wsl.notifyhook.Prefs
import com.wsl.notifyhook.utils.formatAppList

fun buildMainUi(
    context: Context,
    prefs: Prefs,
    onToggle: () -> Unit,
    onShowApps: () -> Unit,
    onShowSettings: () -> Unit,
    onTestWebhook: () -> Unit
): Quadruple<ScrollView, TextView, Button, TextView> {

    val root = ScrollView(context).apply {
        setBackgroundColor(0xFF121212.toInt())
    }
    val content = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(40, 50, 40, 50)
    }
    root.addView(content)

    // Header
    val tvTitle = TextView(context).apply {
        text = "🚀 WSL Listener!"
        textSize = 26f
        setTypeface(null, Typeface.BOLD)
        setTextColor(0xFFFFFFFF.toInt())
        gravity = Gravity.CENTER
    }
    val divider = View(context).apply {
        setBackgroundColor(0xFF333333.toInt())
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 2
        ).apply { topMargin = 20; bottomMargin = 30 }
    }
    val tvStatus = TextView(context).apply {
        textSize = 15f
        text = "⌛ Loading..."
        setTextColor(0xFFBBBBBB.toInt())
        gravity = Gravity.CENTER
    }

    // Toggle
    val btnToggle = modernButton(context, if (prefs.listenerEnabled) "❌ Matikan Listener" else "✅ Nyalakan Listener").apply {
        setOnClickListener { onToggle() }
    }

    // Buttons row
    val row1 = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }
    val btnApps = modernButton(context, "📋 Pilih Aplikasi").apply {
        setOnClickListener { onShowApps() }
    }
    val btnSettings = modernButton(context, "⚙️ Pengaturan").apply {
        setOnClickListener { onShowSettings() }
    }
    row1.addView(btnApps, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 })
    row1.addView(btnSettings, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

    val btnTest = modernButton(context, "📡 TEST WEBHOOK").apply {
        setOnClickListener { onTestWebhook() }
    }

    // App list section
    val tvAppList = TextView(context).apply {
        text = formatAppList(prefs)
        setTextColor(0xFFAAAAAA.toInt())
        textSize = 14f
        setPadding(0, 40, 0, 20)
    }

    // Footer
    val tvFooter = TextView(context).apply {
        text = "© White Space Labs"
        textSize = 13f
        setTextColor(0xFF555555.toInt())
        gravity = Gravity.CENTER
        setPadding(0, 50, 0, 20)
    }

    // Assemble
    listOf(tvTitle, divider, tvStatus, btnToggle, row1, btnTest, tvAppList, tvFooter).forEach {
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 20 }
        content.addView(it, params)
    }

    return Quadruple(root, tvStatus, btnToggle, tvAppList)
}

fun modernButton(context: Context, label: String): Button {
    return Button(context).apply {
        text = label
        textSize = 15f
        setAllCaps(false)
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF1F1B24.toInt())
        setPadding(20, 20, 20, 20)
    }
}

fun modernMiniButton(context: Context, label: String): Button {
    return Button(context).apply {
        text = label
        textSize = 14f
        setAllCaps(false)
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF3700B3.toInt())
    }
}

// Helper class untuk return 4 value
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
