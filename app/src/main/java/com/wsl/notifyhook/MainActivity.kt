package com.wsl.notifyhook

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.*
import com.wsl.notifyhook.ui.*
import com.wsl.notifyhook.utils.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var tvStatus: TextView
    private lateinit var btnToggle: Button
    private lateinit var tvAppList: TextView

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

        setContentView(root)

        ensureNotificationAccess(this, prefs, tvStatus)
        ensureBatteryOptimization(this)

        // ⏰ Schedule background rebind worker tiap 15 menit
        scheduleRebindWorker()

        // 🚀 Start PersistentService biar langsung jalan walau tanpa reboot
        val svc = Intent(this, PersistentService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svc)
        } else {
            startService(svc)
        }
    }

    override fun onResume() {
        super.onResume()
        ensureNotificationAccess(this, prefs, tvStatus)
        ensureBatteryOptimization(this)
        updateToggleText()
        tvAppList.text = formatAppList(prefs)
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
