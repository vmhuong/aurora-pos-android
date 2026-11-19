package com.example.qrorderkitchen.utils

import android.util.Log
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

object NetworkTimeProvider {
    @Volatile
    var offsetMs: Long = 0L
        private set

    init {
        sync()
    }

    fun sync() {
        CoroutineScope(Dispatchers.IO).launch {
            val endpoints = listOf(
                "https://qr-ordering-system-1b7d4.web.app",
                "https://www.google.com",
                "https://firestore.googleapis.com"
            )
            for (endpoint in endpoints) {
                try {
                    val url = URL(endpoint)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "HEAD"
                        connectTimeout = 3500
                        readTimeout = 3500
                        instanceFollowRedirects = false
                    }
                    conn.connect()
                    val serverDate = conn.date
                    conn.disconnect()
                    if (serverDate > 0) {
                        offsetMs = serverDate - System.currentTimeMillis()
                        Log.d("NetworkTimeProvider", "Sync success with $endpoint. Skew offset: ${offsetMs}ms")
                        break
                    }
                } catch (e: Exception) {
                    Log.w("NetworkTimeProvider", "Failed to sync with $endpoint: ${e.message}")
                }
            }
        }
    }

    fun currentTimeMillis(): Long = System.currentTimeMillis() + offsetMs

    fun currentTimestamp(): Timestamp = Timestamp(Date(currentTimeMillis()))
}
