package com.wsl.notifyhook

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private val client = OkHttpClient()

    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var tvAppList: TextView

    data class AppItem(val label: String, val pkg: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 🚀 Paksa dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        Toast.makeText(this, "Created by @whitespacelabs", Toast.LENGTH_SHORT).show()

        val root = ScrollView(this).apply {
            setBackgroundColor(0xFF121212.toInt())
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 50, 40, 50)
        }
        root.addView(content)

        // Header
        val tvTitle = TextView(this).apply {
            text = "🚀 WSL Listener!"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
        }
        val divider = View(this).apply {
            setBackgroundColor(0xFF333333.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { topMargin = 20; bottomMargin = 30 }
        }
        tvStatus = TextView(this).apply {
            textSize = 15f
            text = statusText()
            setTextColor(0xFFBBBBBB.toInt())
            gravity = Gravity.CENTER
        }

        // Toggle
        btnToggle = modernButton(if (prefs.listenerEnabled) "❌ Matikan Listener" else "✅ Nyalakan Listener").apply {
            setOnClickListener { toggleListener() }
        }

        // Buttons row
        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val btnApps = modernButton("📋 Pilih Aplikasi").apply {
            setOnClickListener { showAppsPopup() }
        }
        val btnSettings = modernButton("⚙️ Pengaturan").apply {
            setOnClickListener { showSettingsPopup() }
        }
        row1.addView(btnApps, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = 10 })
        row1.addView(btnSettings, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val btnTest = modernButton("📡 TEST WEBHOOK").apply {
            setOnClickListener {
                val url = prefs.webhookUrl
                val kode = prefs.secret
                if (url.isEmpty() || !url.startsWith("http")) {
                    Toast.makeText(this@MainActivity, "❌ URL invalid, set dulu di Pengaturan", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (kode.isEmpty()) {
                    Toast.makeText(this@MainActivity, "❌ Kode Akses wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sendTestWebhook(url, kode)
            }
        }

        // App list section
        tvAppList = TextView(this).apply {
            text = formatAppList()
            setTextColor(0xFFAAAAAA.toInt())
            textSize = 14f
            setPadding(0, 40, 0, 20)
        }

        // Footer
        val tvFooter = TextView(this).apply {
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

        setContentView(root)

        ensureNotificationAccess()
        ensureBatteryOptimization()
    }

    override fun onResume() {
        super.onResume()
        ensureNotificationAccess()
        ensureBatteryOptimization()
        updateToggleText()
        tvAppList.text = formatAppList()
    }

    // ===== UI Helper =====
    private fun modernButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            setAllCaps(false)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1F1B24.toInt())
            setPadding(20, 20, 20, 20)
        }
    }

    private fun modernMiniButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 14f
            setAllCaps(false)
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF3700B3.toInt())
        }
    }

    // ===== Logic =====
    private fun ensureNotificationAccess() {
        if (!isListenerSystemEnabled()) {
            Toast.makeText(this, "⚠️ Izinkan akses notifikasi agar NotifyHook berfungsi", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        tvStatus.text = statusText()
    }

    private fun isListenerSystemEnabled(): Boolean {
        val cn = ComponentName(this, NotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.split(":").any { it.equals(cn.flattenToString(), ignoreCase = true) }
    }

    private fun ensureBatteryOptimization() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            Toast.makeText(this, "⚠️ Matikan optimasi baterai agar service tidak mati", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun toggleListener() {
        prefs.listenerEnabled = !prefs.listenerEnabled
        updateToggleText()
        val msg = if (prefs.listenerEnabled) "✅ Listener diaktifkan" else "❌ Listener dimatikan"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateToggleText() {
        btnToggle.text = if (prefs.listenerEnabled) "❌ Matikan Listener" else "✅ Nyalakan Listener"
        tvStatus.text = statusText()
    }

    private fun statusText(): String {
        val sys = isListenerSystemEnabled()
        return when {
            !sys -> "⚠️ Akses sistem belum diizinkan"
            prefs.listenerEnabled -> "✅ Listener: ON"
            else -> "❌ Listener: OFF"
        }
    }

    // ===== Popups =====
    private fun showAppsPopup() {
        val apps = loadUserInstalledApps().sortedBy { it.label.lowercase() }
        val selectedSet = prefs.selectedPackages.toMutableSet()

        val allLabels = apps.map { "${it.label} (${it.pkg})" }
        val allPkgs = apps.map { it.pkg }

        var filteredLabels = allLabels.toMutableList()
        var filteredPkgs = allPkgs.toMutableList()

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val searchBox = EditText(this).apply {
            hint = "🔍 Cari aplikasi..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
            setBackgroundColor(0xFF1E1E1E.toInt())
            setPadding(20, 20, 20, 20)
        }
        dialogLayout.addView(searchBox)

        val listView = ListView(this).apply {
            setBackgroundColor(0xFF121212.toInt())
        }
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, filteredLabels)
        listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        listView.adapter = adapter

        fun refreshChecks() {
            for (i in filteredPkgs.indices) {
                listView.setItemChecked(i, selectedSet.contains(filteredPkgs[i]))
            }
        }
        refreshChecks()

        listView.setOnItemClickListener { _, _, position, _ ->
            val pkg = filteredPkgs[position]
            if (listView.isItemChecked(position)) selectedSet.add(pkg) else selectedSet.remove(pkg)
        }

        dialogLayout.addView(listView)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val btnAll = modernMiniButton("✅ All").apply {
            setOnClickListener {
                selectedSet.addAll(allPkgs)
                refreshChecks()
            }
        }
        val btnNone = modernMiniButton("❌ None").apply {
            setOnClickListener {
                selectedSet.clear()
                refreshChecks()
            }
        }
        btnRow.addView(btnAll, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        btnRow.addView(btnNone, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        dialogLayout.addView(btnRow)

        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.lowercase() ?: ""
                filteredLabels = mutableListOf()
                filteredPkgs = mutableListOf()
                apps.forEachIndexed { idx, app ->
                    if (app.label.lowercase().contains(query) || app.pkg.lowercase().contains(query)) {
                        filteredLabels.add(allLabels[idx])
                        filteredPkgs.add(allPkgs[idx])
                    }
                }
                adapter.clear()
                adapter.addAll(filteredLabels)
                adapter.notifyDataSetChanged()
                refreshChecks()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("🎯 Pilih aplikasi")
            .setView(dialogLayout)
            .setPositiveButton("💾 Save") { _, _ ->
                prefs.selectedPackages = selectedSet
                tvAppList.text = formatAppList()
                Toast.makeText(this, "✅ Saved ${selectedSet.size} apps", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
    }

    private fun showSettingsPopup() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(0xFF121212.toInt())
        }

        val etUrl = EditText(this).apply {
            hint = "Webhook URL"
            setText(prefs.webhookUrl)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }
        val etSecret = EditText(this).apply {
            hint = "Kode Akses (wajib)"
            setText(prefs.secret)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF888888.toInt())
        }

        layout.addView(etUrl)
        layout.addView(etSecret)

        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("⚙️ Pengaturan")
            .setView(layout)
            .setPositiveButton("💾 Save") { _, _ ->
                val kode = etSecret.text.toString().trim()
                if (kode.isEmpty()) {
                    Toast.makeText(this, "❌ Kode Akses wajib diisi!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.webhookUrl = etUrl.text.toString().trim()
                prefs.secret = kode
                Toast.makeText(this, "✅ Pengaturan tersimpan!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("❌ Cancel", null)
            .show()
    }

    // ===== Data =====
    private fun loadUserInstalledApps(): List<AppItem> {
        val pm = packageManager
        val infos = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val out = ArrayList<AppItem>(infos.size)
        for (ai in infos) {
            if ((ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0) continue
            if ((ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) continue
            val label = try { pm.getApplicationLabel(ai).toString() } catch (_: Exception) { ai.packageName }
            out.add(AppItem(label, ai.packageName))
        }
        return out
    }

    private fun formatAppList(): String {
        val pkgs = prefs.selectedPackages
        return if (pkgs.isEmpty()) {
            "📭 Tidak ada aplikasi dipilih."
        } else {
            "📱 Aplikasi yang di-listen:\n- " + pkgs.joinToString("\n- ")
        }
    }

    private fun sendTestWebhook(url: String, kode: String) {
        val json = """{"test":"NotifyHook OK","secret":"$kode"}"""
        val body = json.toRequestBody("application/json".toMediaTypeOrNull())

        val req = Request.Builder().url(url).post(body).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "❌ Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    when {
                        response.isSuccessful -> {
                            Toast.makeText(this@MainActivity, "✅ Webhook OK", Toast.LENGTH_SHORT).show()
                        }
                        response.code == 401 -> {
                            Toast.makeText(
                                this@MainActivity,
                                "❌ ANDA TIDAK TERDAFTAR!\nHubungi @whitespacelabs",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            Toast.makeText(
                                this@MainActivity,
                                "⚠️ HTTP ${response.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                response.close()
            }
        })
    }
}
