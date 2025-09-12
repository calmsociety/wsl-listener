package com.wsl.notifyhook

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.wsl.notifyhook.ui.*
import com.wsl.notifyhook.utils.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var tvAppList: TextView
    private lateinit var tvLastLog: TextView

    // 📡 Receiver untuk update log realtime
    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val newLog = intent?.getStringExtra("lastLog") ?: return
            tvLastLog.text = newLog
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        Toast.makeText(this, "Created by @whitespacelabs", Toast.LENGTH_SHORT).show()

        // 🏗️ Build UI pakai UiHelper
        val (root, statusView, toggleBtn, appList) = buildMainUi(
            context = this,
            prefs = prefs,
            onToggle = { toggleListener() },
            onShowApps = { showAppsPopup(this, prefs, tvAppList) },
            onShowSettings = { showSettingsPopup(this, prefs) },
            onTestWebhook = { sendTestWebhook(this, prefs) }
        )

        tvStatus = statusView
        btnToggle = toggleBtn
        tvAppList = appList

        // 🔥 TextView untuk log terakhir
        tvLastLog = TextView(this).apply {
            text = prefs.lastLog
            textSize = 14f
            setPadding(20, 50, 20, 20)
            setTextColor(0xFFAAAAAA.toInt())
        }

        // 👇 Root = ScrollView, ambil child pertama = LinearLayout
        val scrollRoot = root as android.widget.ScrollView
        val innerLayout = scrollRoot.getChildAt(0) as android.widget.LinearLayout
        innerLayout.addView(tvLastLog)

        setContentView(scrollRoot)

        ensureNotificationAccess(this, prefs, tvStatus)
        ensureBatteryOptimization(this)
        scheduleRebindWorker()

        // 🚀 Start PersistentService biar langsung jalan
        val svc = Intent(this, PersistentService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }

        // 📡 register receiver untuk update log realtime
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(logReceiver, IntentFilter("WSL_NEW_LOG"))
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logReceiver)
    }

    override fun onResume() {
        super.onResume()
        ensureNotificationAccess(this, prefs, tvStatus)
        ensureBatteryOptimization(this)
        updateToggleText()
        tvAppList.text = formatAppList(prefs)

        // 🔄 update log terakhir dari prefs
        tvLastLog.text = prefs.lastLog
    }

    private fun toggleListener() {
        prefs.listenerEnabled = !prefs.listenerEnabled
        updateToggleText()
        val msg = if (prefs.listenerEnabled) "✅ Listener diaktifkan" else "❌ Listener dimatikan"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun updateToggleText() {
        btnToggle.text = if (prefs.listenerEnabled) "❌ Matikan Listener" else "✅ Nyalakan Listener"
        tvStatus.text = statusText(this, prefs)
    }

    private fun scheduleRebindWorker() {
        val req = PeriodicWorkRequestBuilder<ListenerRebindWorker>(
            15, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(false)
                .setRequiresCharging(false)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "listener_rebind",
            ExistingPeriodicWorkPolicy.KEEP,
            req
        )
    }
}
