package com.qian.scrollsanity.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.qian.scrollsanity.R
import com.qian.scrollsanity.data.UsageSyncHelper
import kotlinx.coroutines.*

/**
 * Foreground service that syncs usage data to Firestore every 30 seconds
 */
class UsageSyncService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var syncJob: Job? = null
    private lateinit var syncHelper: UsageSyncHelper

    companion object {
        private const val TAG = "UsageSyncService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_sync_channel"
        private const val SYNC_INTERVAL_MS = 30_000L // 30 seconds

        fun start(context: Context) {
            val intent = Intent(context, UsageSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, UsageSyncService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        syncHelper = UsageSyncHelper(this)
        createNotificationChannel()
        Log.d(TAG, "UsageSyncService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Start periodic sync
        startPeriodicSync()

        Log.d(TAG, "UsageSyncService started - syncing every 30 seconds")
        return START_STICKY // Restart service if killed
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        syncJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "UsageSyncService destroyed")
    }

    private fun startPeriodicSync() {
        syncJob?.cancel() // Cancel any existing job

        syncJob = serviceScope.launch {
            while (isActive) {
                try {
                    Log.d(TAG, "Syncing usage data...")
                    syncHelper.performPeriodicSync()
                    Log.d(TAG, "Sync completed successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Sync failed", e)
                }

                // Wait 30 seconds before next sync
                delay(SYNC_INTERVAL_MS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Usage Sync",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Syncs your app usage data to the cloud"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Arete")
            .setContentText("Syncing usage data")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using default launcher icon
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
