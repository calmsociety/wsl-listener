package com.wsl.notifyhook

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

class NotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 🚦 cek toggle listener
        if (!prefs.listenerEnabled) {
            Log.d("WSLListener", "⏸ Listener OFF → notif diabaikan: ${sbn.packageName}")
            return
        }

        val pkg = sbn.packageName ?: return

        // 🎯 cek whitelist
        if (!prefs.isWhitelisted(pkg)) {
            Log.d("WSLListener", "🚫 App $pkg tidak ada di whitelist → skip")
            return
        }

        // 🚦 cek kode akses wajib
        val kodeAkses = prefs.secret
        if (kodeAkses.isEmpty()) {
            Log.w("WSLListener", "⚠️ Kode Akses kosong → notif tidak dikirim")
            return
        }

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val text = extras?.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val ticker = sbn.notification.tickerText?.toString()?.trim() ?: ""

        val payload = JSONObject().apply {
            put("package", pkg)
            put("title", title)
            put("text", text)
            put("ticker", ticker)
            put("secret", kodeAkses)
        }

        val url = prefs.webhookUrl
        if (url.isEmpty()) {
            Log.w("WSLListener", "⚠️ Webhook URL kosong → notif tidak dikirim")
            return
        }

        val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder().url(url).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("WSLListener", "❌ Gagal kirim notif $pkg → ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("WSLListener", "✅ Notif $pkg terkirim (${response.code})")
                } else {
                    Log.w("WSLListener", "⚠️ Notif $pkg gagal (${response.code})")
                }
                response.close()
            }
        })
    }
}
