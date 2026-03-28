package com.qian.scrollsanity.data.local.usagestats

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.qian.scrollsanity.domain.model.dashboard.AppCategory
import com.qian.scrollsanity.domain.model.dashboard.AppUsageInfo
import com.qian.scrollsanity.domain.model.dashboard.DailyUsageInfo
import com.qian.scrollsanity.domain.model.session.AppSession

import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.model.session.TrackedAppMeta
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.util.TrackedApps
import java.util.Calendar
import kotlin.math.max

/**
 * Repository for reading local app-usage data from Android UsageStats / UsageEvents.
 *
 * Design split:
 * 1. Dashboard / daily totals:
 *    - use UsageStatsManager.queryUsageStats(...)
 *    - more stable for total foreground time
 *    - avoids fragile manual reconstruction for today's aggregate usage
 *
 * 2. Intervention / session analysis:
 *    - use UsageEvents
 *    - reconstruct sessions explicitly for z-score / trigger logic
 */
class UsageStatsRepository(
    private val context: Context
) : LocalUsageRepo {

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val packageManager: PackageManager = context.packageManager

    companion object {
        private const val TAG_USAGE_DEBUG = "USAGE_DEBUG"
        private const val TAG_USAGE_RESULT = "USAGE_RESULT"
        private const val TAG_SESSION_DEBUG = "SESSION_DEBUG"
    }

    /**
     * Returns today's total minutes across enabled tracked apps.
     *
     * Used by UI / daily summary paths.
     */
    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int {
        return getTodayUsageStats(enabled).sumOf { it.usageTimeMinutes }
    }

    /**
     * Returns previous 7 full-day totals, excluding today.
     *
     * Behavior:
     * - fetch 8 day buckets total
     * - index 0 = today
     * - drop today
     * - keep the previous 7 full days
     */
    override suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int> {
        return getUsageStatsForDays(days = 8, enabled = enabled)
            .drop(1)
            .take(7)
            .map { it.totalMinutes }
    }

    /**
     * Returns reconstructed tracked sessions for the recent [days] window.
     *
     * This remains UsageEvents-based because intervention logic depends on sessions,
     * not only aggregate foreground time.
     */
    override suspend fun getRecentSessions(
        enabledPackages: Set<String>,
        days: Int
    ): List<AppSession> {
        return getSessionsForDays(
            days = days,
            enabledPackages = enabledPackages
        )
    }

    /**
     * Reconstructs tracked-app sessions from UsageEvents.
     *
     * Notes:
     * - only tracked apps in [enabledPackages] are considered
     * - sessions shorter than 1 minute are discarded
     * - any open session at query end is closed at [end]
     *
     * This function is kept for intervention/session analysis.
     * It is no longer the source of truth for Dashboard total usage.
     */
    fun getSessionsForDays(
        days: Int,
        enabledPackages: Set<String>
    ): List<AppSession> {
        if (days <= 0) return emptyList()

        val start = startOfDayMillis(daysAgo = days - 1)
        val end = System.currentTimeMillis()

        Log.d(
            TAG_SESSION_DEBUG,
            "getSessionsForDays days=$days start=$start end=$end enabledPackages=$enabledPackages"
        )

        val enabledMetas = TrackedApps.all.filter { meta ->
            meta.exactPackages.any { it in enabledPackages }
        }

        if (enabledMetas.isEmpty()) {
            Log.d(TAG_SESSION_DEBUG, "No enabled tracked metas resolved for session query")
            return emptyList()
        }

        val exactPkgToId: Map<String, TrackedAppId> =
            enabledMetas
                .flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }
                .toMap()

        fun resolveTrackedId(packageName: String): TrackedAppId? {
            exactPkgToId[packageName]?.let { return it }

            val lower = packageName.lowercase()
            for (meta in enabledMetas) {
                if (meta.fallbackContains.any { lower.contains(it) }) {
                    return meta.id
                }
            }
            return null
        }

        val events: UsageEvents = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()

        val sessions = mutableListOf<AppSession>()

        var currentPkg: String? = null
        var currentTrackedId: TrackedAppId? = null
        var currentStart: Long? = null

        fun closeCurrent(at: Long) {
            val pkg = currentPkg
            val trackedId = currentTrackedId
            val startTime = currentStart

            if (pkg != null && trackedId != null && startTime != null) {
                val durationMillis = (at - startTime).coerceAtLeast(0L)

                if (durationMillis >= 60_000L) {
                    val durationMinutes = (durationMillis / 60_000L).toInt()

                    sessions.add(
                        AppSession(
                            trackedAppId = trackedId,
                            packageName = pkg,
                            startMillis = startTime,
                            endMillis = at,
                            durationMinutes = durationMinutes
                        )
                    )

                    Log.d(
                        TAG_SESSION_DEBUG,
                        "Session closed trackedId=$trackedId pkg=$pkg start=$startTime end=$at durationMin=$durationMinutes"
                    )
                }
            }

            currentPkg = null
            currentTrackedId = null
            currentStart = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val pkg = event.packageName ?: continue
            val t = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val trackedId = resolveTrackedId(pkg)

                    if (trackedId != null) {
                        if (currentPkg != null && currentPkg != pkg) {
                            closeCurrent(t)
                        }

                        currentPkg = pkg
                        currentTrackedId = trackedId
                        currentStart = max(t, start)
                    }
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentPkg == pkg && currentStart != null) {
                        closeCurrent(t)
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    closeCurrent(t)
                }
            }
        }

        if (currentPkg != null && currentStart != null) {
            closeCurrent(end)
        }

        Log.d(TAG_SESSION_DEBUG, "getSessionsForDays resultCount=${sessions.size}")
        return sessions
    }

    /**
     * Returns per-app usage stats for today.
     *
     * IMPORTANT:
     * - This is now based on queryUsageStats aggregate foreground time.
     * - This is the main source for Dashboard / daily total / sync.
     */
    fun getTodayUsageStats(
        enabled: Set<TrackedAppId> = TrackedApps.allIds
    ): List<AppUsageInfo> {
        val start = startOfDayMillis(daysAgo = 0)
        val end = System.currentTimeMillis()

        Log.d(
            TAG_USAGE_DEBUG,
            "getTodayUsageStats start=$start end=$end enabled=$enabled"
        )

        return getUsageStatsFromAggregates(start, end, enabled)
    }

    /**
     * Returns daily usage summaries for the last [days] day buckets.
     *
     * Ordering:
     * - index 0 = today
     * - index 1 = yesterday
     * - ...
     */
    fun getUsageStatsForDays(
        days: Int,
        enabled: Set<TrackedAppId> = TrackedApps.allIds
    ): List<DailyUsageInfo> {
        if (days <= 0) return emptyList()

        val now = System.currentTimeMillis()
        val result = mutableListOf<DailyUsageInfo>()

        for (i in 0 until days) {
            val start = startOfDayMillis(daysAgo = i)
            val end = minOf(endOfDayMillis(daysAgo = i), now)

            val apps = getUsageStatsFromAggregates(start, end, enabled)
            val totalMinutes = apps.sumOf { it.usageTimeMinutes }

            result.add(
                DailyUsageInfo(
                    date = start,
                    totalMinutes = totalMinutes,
                    apps = apps
                )
            )
        }

        return result
    }

    /**
     * Aggregate per-app tracked usage from Android UsageStats.
     *
     * Why use this for Dashboard:
     * - more robust for total foreground time
     * - less sensitive to foreground/background pairing gaps
     * - better behavior across restart / window boundary scenarios
     */
    private fun getUsageStatsFromAggregates(
        startTime: Long,
        endTime: Long,
        enabled: Set<TrackedAppId>
    ): List<AppUsageInfo> {
        if (endTime <= startTime) return emptyList()

        val enabledMetas = TrackedApps.all.filter { it.id in enabled }
        if (enabledMetas.isEmpty()) {
            Log.d(TAG_USAGE_DEBUG, "No enabled tracked metas for aggregate query")
            return emptyList()
        }

        val exactPkgToId: Map<String, TrackedAppId> =
            enabledMetas
                .flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }
                .toMap()

        fun resolveTrackedId(packageName: String): TrackedAppId? {
            exactPkgToId[packageName]?.let { return it }

            val lower = packageName.lowercase()
            for (meta in enabledMetas) {
                if (meta.fallbackContains.any { lower.contains(it) }) {
                    return meta.id
                }
            }
            return null
        }

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        Log.d(
            TAG_USAGE_DEBUG,
            "queryUsageStats returned count=${usageStats.size} for start=$startTime end=$endTime"
        )

        val installedEnabledMetas = enabledMetas.filter { isTrackedAppInstalled(it) }

        val resultsById: MutableMap<TrackedAppId, AppUsageInfo> =
            installedEnabledMetas.associate { meta ->
                val representativePkg =
                    meta.exactPackages.firstOrNull() ?: meta.displayName.lowercase()

                meta.id to AppUsageInfo(
                    packageName = representativePkg,
                    appName = meta.displayName,
                    usageTimeMinutes = 0,
                    lastTimeUsed = 0L,
                    category = AppCategory.SOCIAL
                )
            }.toMutableMap()

        usageStats.forEach { stat ->
            val pkg = stat.packageName ?: return@forEach
            val id = resolveTrackedId(pkg) ?: return@forEach
            val meta = TrackedApps.metaFor(id)

            val totalForegroundMs = getForegroundMsCompat(stat)
            if (totalForegroundMs <= 0L) return@forEach

            val minutes = (totalForegroundMs / 60_000L).toInt()
            val usedAt = stat.lastTimeUsed

            Log.d(
                TAG_USAGE_DEBUG,
                "aggregateHit app=${meta.displayName}, pkg=$pkg, foregroundMs=$totalForegroundMs, minutes=$minutes, lastUsed=$usedAt"
            )

            val existing = resultsById[id]
            if (existing != null) {
                resultsById[id] = existing.copy(
                    usageTimeMinutes = existing.usageTimeMinutes + minutes,
                    lastTimeUsed = max(existing.lastTimeUsed, usedAt),
                    packageName = if (existing.packageName.startsWith("com.")) {
                        existing.packageName
                    } else {
                        pkg
                    },
                    appName = meta.displayName,
                    category = AppCategory.SOCIAL
                )
            } else {
                resultsById[id] = AppUsageInfo(
                    packageName = pkg,
                    appName = meta.displayName,
                    usageTimeMinutes = minutes,
                    lastTimeUsed = usedAt,
                    category = AppCategory.SOCIAL
                )
            }
        }

        resultsById.values.forEach {
            Log.d(
                TAG_USAGE_RESULT,
                "app=${it.appName}, pkg=${it.packageName}, minutes=${it.usageTimeMinutes}, lastUsed=${it.lastTimeUsed}"
            )
        }

        return installedEnabledMetas
            .mapNotNull { resultsById[it.id] }
            .sortedByDescending { it.usageTimeMinutes }
    }

    /**
     * Android foreground-time compatibility helper.
     *
     * On Android Q+:
     * - totalTimeVisible may be more stable for user-visible time in some cases
     * - totalTimeInForeground still exists and is widely used
     *
     * To stay conservative and compatible with your existing logic, we prefer
     * foreground time, but on Q+ we take the larger of foreground/visible.
     */
    private fun getForegroundMsCompat(stat: UsageStats): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            max(stat.totalTimeInForeground, stat.totalTimeVisible)
        } else {
            stat.totalTimeInForeground
        }
    }

    /**
     * Returns true if the package is installed on this device.
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Returns true if any exact package for this tracked app is installed.
     */
    private fun isTrackedAppInstalled(meta: TrackedAppMeta): Boolean {
        return meta.exactPackages.any { isAppInstalled(it) }
    }

    /**
     * Returns local start-of-day timestamp for [daysAgo].
     *
     * 0 = today 00:00:00.000
     * 1 = yesterday 00:00:00.000
     */
    private fun startOfDayMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Returns local end-of-day timestamp for [daysAgo].
     */
    private fun endOfDayMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}