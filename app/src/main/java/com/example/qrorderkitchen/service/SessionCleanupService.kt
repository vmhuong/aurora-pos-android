package com.example.qrorderkitchen.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.qrorderkitchen.data.repository.AuroraRepository

class SessionCleanupService : Service() {

    companion object {
        private const val TAG = "SessionCleanupService"
        private const val PREF_NAME = "aurora_active_session"
        private const val KEY_ACTIVE_USERNAME = "active_staff_username"
        private const val ACTION_START = "ACTION_START_SESSION"
        private const val ACTION_STOP = "ACTION_STOP_SESSION"
        private const val EXTRA_USERNAME = "EXTRA_USERNAME"

        fun start(context: Context, username: String) {
            try {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_ACTIVE_USERNAME, username)
                    .commit()

                val intent = Intent(context, SessionCleanupService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_USERNAME, username)
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SessionCleanupService: ${e.message}")
            }
        }

        fun stop(context: Context) {
            try {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()

                val intent = Intent(context, SessionCleanupService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SessionCleanupService: ${e.message}")
            }
        }

        fun getActiveStaffUsername(context: Context): String? {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_USERNAME, null)
        }

        fun clearSession(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "onTaskRemoved triggered: app swiped away from Recents!")

        val username = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_USERNAME, null)

        if (!username.isNullOrBlank()) {
            val thread = Thread {
                try {
                    AuroraRepository().releaseAllTablesForStaffBlocking(username, timeoutSec = 3)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in onTaskRemoved table release: ${e.message}")
                } finally {
                    clearSession(applicationContext)
                }
            }
            thread.start()
            try {
                thread.join(3000)
            } catch (_: InterruptedException) {}
        } else {
            clearSession(applicationContext)
        }

        stopSelf()
    }
}
