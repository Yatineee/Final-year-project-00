package com.qian.scrollsanity.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper class to sync usage statistics to Firestore
 * Matches Chrome extension structure: users/{userId}/apps/{appId}
 *
 * Tracks deltas in SharedPreferences to survive across worker invocations
 */
class UsageSyncHelper(private val context: Context) {
    private val firestoreRepo = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val usageRepo = UsageStatsRepository(context)

    // Persist last synced values to SharedPreferences
    private val prefs = context.getSharedPreferences("usage_sync", Context.MODE_PRIVATE)

    // Map package names or display names → lowercase app IDs (matches extension)
    private val APP_ID_MAP = mapOf(
        "com.instagram.android" to "instagram",
        "Instagram" to "instagram",
        "com.facebook.katana" to "facebook",
        "Facebook" to "facebook",
        "com.zhiliaoapp.musically" to "tiktok",
        "TikTok" to "tiktok",
        "com.google.android.youtube" to "youtube",
        "YouTube" to "youtube",
        "com.reddit.frontpage" to "reddit",
        "Reddit" to "reddit",
        "com.snapchat.android" to "snapchat",
        "Snapchat" to "snapchat",
        "com.twitter.android" to "twitter",
        "Twitter" to "twitter",
        "X" to "twitter"
    )

    private fun getAppId(appName: String): String? {
        return APP_ID_MAP[appName] ?: appName.lowercase(Locale.US).takeIf { it in APP_ID_MAP.values }
    }

    // Get last synced seconds for an app from SharedPreferences
    private fun getLastSyncedSeconds(appId: String): Long {
        return prefs.getLong("last_synced_$appId", 0L)
    }

    // Save last synced seconds for an app to SharedPreferences
    private fun setLastSyncedSeconds(appId: String, seconds: Long) {
        prefs.edit().putLong("last_synced_$appId", seconds).apply()
    }

    /**
     * Sync today's usage data to Firestore (per-app incremental updates)
     * Uses SharedPreferences to track deltas across invocations
     */
    fun syncTodayUsage() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val todayStats = usageRepo.getTodayUsageStats()
                if (todayStats.isEmpty()) return@launch

                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                // Sync each app individually using delta calculation
                todayStats.forEach { stat ->
                    val appId = getAppId(stat.appName) ?: return@forEach
                    val currentSeconds = (stat.usageTimeMinutes * 60).toLong()
                    val lastSeconds = getLastSyncedSeconds(appId)

                    // Only sync the delta (new time since last sync)
                    val deltaSeconds = currentSeconds - lastSeconds

                    if (deltaSeconds > 0) {
                        firestoreRepo.incrementAppUsage(
                            userId = userId,
                            appId = appId,
                            deltaSeconds = deltaSeconds,
                            date = dateStr
                        )
                        setLastSyncedSeconds(appId, currentSeconds)
                        Log.d("UsageSyncHelper", "Synced $appId: +${deltaSeconds}s (total: ${currentSeconds}s)")
                    }
                }

                Log.d("UsageSyncHelper", "Today's usage synced for ${todayStats.size} apps")
            } catch (e: Exception) {
                Log.e("UsageSyncHelper", "Failed to sync today's usage", e)
            }
        }
    }

    /**
     * Sync usage delta for a specific app (matches Chrome extension USAGE_DELTA)
     * @param appId The app display name (e.g., "Instagram", not "com.instagram.android")
     */
    fun syncAppUsageDelta(
        appId: String,
        deltaSeconds: Long,
        date: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    ) {
        val userId = firebaseAuth.currentUser?.uid ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val normalizedAppId = getAppId(appId) ?: appId.lowercase(Locale.US)
                firestoreRepo.incrementAppUsage(
                    userId = userId,
                    appId = normalizedAppId,
                    deltaSeconds = deltaSeconds,
                    date = date
                )
                Log.d("UsageSyncHelper", "App usage delta synced: $normalizedAppId (+${deltaSeconds}s)")
            } catch (e: Exception) {
                Log.e("UsageSyncHelper", "Failed to sync app usage delta", e)
            }
        }
    }

    /**
     * Periodic sync - only syncs the DELTA since last sync
     * Uses SharedPreferences to persist state across worker invocations
     */
    suspend fun performPeriodicSync() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            // Check if date changed; if so, reset all last synced values
            val lastSyncDate = prefs.getString("last_sync_date", dateStr) ?: dateStr
            if (dateStr != lastSyncDate) {
                Log.d("UsageSyncHelper", "New day detected, resetting sync tracking")
                prefs.edit().clear().apply()
                prefs.edit().putString("last_sync_date", dateStr).apply()
            }

            val todayStats = usageRepo.getTodayUsageStats()
            if (todayStats.isEmpty()) return

            var syncCount = 0
            var totalDelta = 0L

            // Sync only the delta for each app
            todayStats.forEach { stat ->
                val appId = getAppId(stat.appName) ?: return@forEach
                val currentSeconds = (stat.usageTimeMinutes * 60).toLong()
                val lastSeconds = getLastSyncedSeconds(appId)

                // Calculate delta (how much time was added since last sync)
                val deltaSeconds = currentSeconds - lastSeconds

                if (deltaSeconds > 0) {
                    firestoreRepo.incrementAppUsage(
                        userId = userId,
                        appId = appId,
                        deltaSeconds = deltaSeconds,
                        date = dateStr
                    )

                    // Persist the last synced value
                    setLastSyncedSeconds(appId, currentSeconds)

                    syncCount++
                    totalDelta += deltaSeconds
                    Log.d("UsageSyncHelper", "$appId: +${deltaSeconds}s")
                }
            }

            if (syncCount > 0) {
                Log.d("UsageSyncHelper", "Periodic sync: ${syncCount} apps, +${totalDelta}s total delta")
            } else {
                Log.d("UsageSyncHelper", "Periodic sync: no new usage to sync")
            }
        } catch (e: Exception) {
            Log.e("UsageSyncHelper", "Periodic sync failed", e)
        }
    }

    /**
     * Get all app usage from Firestore for today
     */
    suspend fun getTodayUsageFromFirestore(): List<com.qian.scrollsanity.domain.model.AppUsageData> {
        val userId = firebaseAuth.currentUser?.uid ?: return emptyList()

        return try {
            val result = firestoreRepo.getAllAppUsageToday(userId)
            result.getOrNull() ?: emptyList()
        } catch (e: Exception) {
            Log.e("UsageSyncHelper", "Failed to get today's usage from Firestore", e)
            emptyList()
        }
    }

    /**
     * Reset tracking (call when user logs out)
     */
    fun reset() {
        prefs.edit().clear().apply()
    }
}