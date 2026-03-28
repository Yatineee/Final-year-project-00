package com.qian.scrollsanity.data.old.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.data.remote.firestore.UsageStatsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsageSyncHelper(
    private val context: Context
) {
    private val firestoreRepo = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val usageRepo = UsageStatsRepository(context)

    private val prefs = context.getSharedPreferences("usage_sync", Context.MODE_PRIVATE)

    private val appIdMap = mapOf(
        "Instagram" to "instagram",
        "Facebook" to "facebook",
        "TikTok" to "tiktok",
        "YouTube" to "youtube",
        "Reddit" to "reddit",
        "Snapchat" to "snapchat",
        "Twitter" to "twitter",
        "X" to "twitter"
    )

    private fun getAppId(appName: String): String? {
        return appIdMap[appName] ?: appName.lowercase(Locale.US).takeIf { it in appIdMap.values }
    }

    private fun getLastSyncedSeconds(appId: String): Long {
        return prefs.getLong("last_synced_$appId", 0L)
    }

    private fun setLastSyncedSeconds(appId: String, seconds: Long) {
        prefs.edit().putLong("last_synced_$appId", seconds).apply()
    }

    suspend fun performPeriodicSync() = withContext(Dispatchers.IO) {
        val userId = firebaseAuth.currentUser?.uid ?: return@withContext

        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            val lastSyncDate = prefs.getString("last_sync_date", dateStr) ?: dateStr
            if (dateStr != lastSyncDate) {
                Log.d("UsageSyncHelper", "New day detected, resetting sync tracking")
                prefs.edit().clear().apply()
                prefs.edit().putString("last_sync_date", dateStr).apply()
            }

            val todayStats = usageRepo.getTodayUsageStats()
            Log.d("SYNC_DEBUG", "todayStats=$todayStats")

            if (todayStats.isEmpty()) return@withContext

            var syncCount = 0
            var totalDelta = 0L

            todayStats.forEach { stat ->
                val appId = getAppId(stat.appName) ?: return@forEach
                val currentSeconds = (stat.usageTimeMinutes * 60).toLong()
                val lastSeconds = getLastSyncedSeconds(appId)
                val deltaSeconds = currentSeconds - lastSeconds

                Log.d(
                    "SYNC_DEBUG",
                    "app=${stat.appName}, currentSeconds=$currentSeconds, lastSeconds=$lastSeconds, deltaSeconds=$deltaSeconds"
                )

                if (deltaSeconds > 0) {
                    firestoreRepo.incrementAppUsage(
                        userId = userId,
                        appId = appId,
                        deltaSeconds = deltaSeconds,
                        date = dateStr
                    )

                    setLastSyncedSeconds(appId, currentSeconds)

                    syncCount++
                    totalDelta += deltaSeconds
                    Log.d("UsageSyncHelper", "$appId: +${deltaSeconds}s")
                }
            }

            if (syncCount > 0) {
                Log.d("UsageSyncHelper", "Periodic sync: $syncCount apps, +${totalDelta}s total delta")
            } else {
                Log.d("UsageSyncHelper", "Periodic sync: no new usage to sync")
            }
        } catch (e: Exception) {
            Log.e("UsageSyncHelper", "Periodic sync failed", e)
        }
    }

    fun reset() {
        prefs.edit().clear().apply()
    }
}