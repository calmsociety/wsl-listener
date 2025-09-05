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
    private val TAG = "WSLListener"

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        Log.i(TAG, "🚀 NotificationListener onCreate()")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: "unknown"
        Log.d(TAG, "📩 Notif masuk dari: $pkg")

        // 🚦 cek toggle listener
        if (!prefs.listenerEnabled) {
            Log.d(TAG, "⏸ Listener OFF → notif diabaikan ($pkg)")
            return
        }

        // 🎯 cek whitelist
        val whitelist = prefs.selectedPackages
        if (!prefs.isWhitelisted(pkg)) {
            Log.d(TAG, "🚫 $pkg tidak ada di whitelist → skip. Current whitelist=$whitelist")
            return
        }

        // 🚦 cek accessCode wajib
        val kodeAkses = prefs.accessCode
        if (kodeAkses.isEmpty()) {
            Log.w(TAG, "⚠️ accessCode kosong → notif tidak dikirim ($pkg)")
            return
        }

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val text = extras?.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val ticker = sbn.notification.tickerText?.toString()?.trim() ?: ""

        // 🛑 Skip notif kosong
        if (title.isBlank() && text.isBlank() && ticker.isBlank()) {
            Log.d(TAG, "⚠️ Notif kosong dari $pkg → di-skip")
            return
        }

        // 📦 Build JSON payload
        val payload = JSONObject().apply {
            put("package", pkg)
            put("title", title)
            put("text", text)
            put("ticker", ticker)
            put("accessCode", kodeAkses)
        }

        Log.d(TAG, "📝 Payload siap dikirim: $payload")

        val url = "https://wsl.biz.id/notif.php" // 🔒 fixed webhook URL
        val body = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder().url(url).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "❌ Gagal kirim notif $pkg → ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i(TAG, "✅ Notif $pkg terkirim (${response.code})")
                } else {
                    Log.w(TAG, "⚠️ Notif $pkg gagal (${response.code}) body=${response.body?.string()}")
                }
                response.close()
            }
        })
    }
}
