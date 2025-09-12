package com.wsl.notifyhook

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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

        if (!prefs.listenerEnabled) return
        if (!prefs.isWhitelisted(pkg)) return
        val kodeAkses = prefs.accessCode
        if (kodeAkses.isEmpty()) return

        val extras = sbn.notification.extras
        val title = extras?.getCharSequence("android.title")?.toString()?.trim() ?: ""
        val text = extras?.getCharSequence("android.text")?.toString()?.trim() ?: ""
        val ticker = sbn.notification.tickerText?.toString()?.trim() ?: ""

        if (title.isBlank() && text.isBlank() && ticker.isBlank()) return

        // 🕒 Format waktu sekarang
        val waktu = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        // 🔥 simpan log terakhir
        prefs.lastLog = "🕒 $waktu\n📦 $pkg\n📝 $title\n💬 $text"

        // 🚀 kirim broadcast biar MainActivity update realtime
        val intent = Intent("WSL_NEW_LOG")
        intent.putExtra("lastLog", prefs.lastLog)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

        // 📦 build JSON payload
        val payload = JSONObject().apply {
            put("package", pkg)
            put("title", title)
            put("text", text)
            put("ticker", ticker)
            put("accessCode", kodeAkses)
            put("time", waktu)
        }

        val url = "https://wsl.biz.id/notif.php"
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
