# ✅ Keep WorkManager & OkHttp
-keep class androidx.work.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ✅ Keep app core classes
-keep class com.wsl.notifyhook.Prefs { *; }
-keep class com.wsl.notifyhook.NotificationListener { *; }
-keep class com.wsl.notifyhook.BootReceiver { *; }
-keep class com.wsl.notifyhook.MainActivity { *; }

# ✅ Keep UI + utils helpers
-keep class com.wsl.notifyhook.ui.** { *; }
-keep class com.wsl.notifyhook.utils.** { *; }

# ✅ Keep Kotlin metadata (biar refleksi aman)
-keep class kotlin.Metadata { *; }

# ⚡ Optional: jaga nama JSON key
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
