package com.wsl.notifyhook

import android.content.Context
import android.util.Log
import androidx.work.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebhookWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    data class Payload(
        val pkg: String,
        val appName: String,
        val title: String,
        val text: String?,
        val ticker: String?,
        val whenMs: Long,
        val extras: Map<String, String>,
        val accessCode: String
    )

    override suspend fun doWork(): Result {
        val accessCode = inputData.getString("accessCode").orEmpty()
        if (accessCode.isBlank()) {
            Log.w("WebhookWorker", "⚠️ accessCode kosong — skip")
            return Result.success()
        }

        // 🔒 Fixed webhook URL
        val url = "https://wsl.biz.id/notif.php"

        val json = JSONObject().apply {
            put("package", inputData.getString("pkg").orEmpty())
            put("app_name", inputData.getString("appName").orEmpty())
            put("title", inputData.getString("title").orEmpty())
            put("text", inputData.getString("text").orEmpty())
            put("ticker", inputData.getString("ticker").orEmpty())
            put("when", inputData.getLong("whenMs", 0L))
            put("extras", JSONObject(inputData.getString("extrasJson").orEmpty()))
            put("accessCode", accessCode)
        }

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        return try {
            val client = OkHttpClient.Builder()
                .callTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            client.newCall(req).execute().use { resp ->
                Log.i("WebhookWorker", "✅ POST ${resp.code}")
                if (resp.isSuccessful) Result.success() else Result.retry()
            }
        } catch (e: Exception) {
            Log.e("WebhookWorker", "❌ Send error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context, payload: Payload) {
            val extrasJson = JSONObject().apply {
                payload.extras.forEach { (k, v) -> put(k, v) }
            }.toString()

            val data = workDataOf(
                "pkg" to payload.pkg,
                "appName" to payload.appName,
                "title" to payload.title,
                "text" to (payload.text ?: ""),
                "ticker" to (payload.ticker ?: ""),
                "whenMs" to payload.whenMs,
                "extrasJson" to extrasJson,
                "accessCode" to payload.accessCode
            )

            val req = OneTimeWorkRequestBuilder<WebhookWorker>()
                .setInputData(data)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(req)
            Log.d("WebhookWorker", "📦 Enqueued id=${req.id}")
        }
    }
}
