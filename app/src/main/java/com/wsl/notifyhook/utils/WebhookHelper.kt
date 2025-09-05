package com.wsl.notifyhook.ui

import android.content.Context
import android.widget.Toast
import com.wsl.notifyhook.Prefs
import okhttp3.*
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

private val client = OkHttpClient()

fun sendTestWebhook(context: Context, prefs: Prefs) {
    val kode = prefs.accessCode
    if (kode.isEmpty()) {
        Toast.makeText(context, "❌ Kode Akses wajib diisi!", Toast.LENGTH_SHORT).show()
        return
    }

    val url = "https://wsl.biz.id/notif.php"
    val json = """{"test":"NotifyHook OK","accessCode":"$kode"}"""
    val body = json.toRequestBody("application/json".toMediaTypeOrNull())

    val req = Request.Builder().url(url).post(body).build()
    client.newCall(req).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            (context as? android.app.Activity)?.runOnUiThread {
                Toast.makeText(context, "❌ Gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        override fun onResponse(call: Call, response: Response) {
            (context as? android.app.Activity)?.runOnUiThread {
                when {
                    response.isSuccessful -> {
                        Toast.makeText(context, "✅ Webhook OK", Toast.LENGTH_SHORT).show()
                    }
                    response.code == 401 -> {
                        Toast.makeText(
                            context,
                            "❌ ANDA TIDAK TERDAFTAR!\nHubungi @whitespacelabs",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        Toast.makeText(
                            context,
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
