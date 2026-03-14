package com.qian.scrollsanity.data.usagedata

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import com.qian.scrollsanity.data.TrackedAppId
import com.qian.scrollsanity.data.TrackedAppMeta
import com.qian.scrollsanity.data.TrackedApps
import com.qian.scrollsanity.domain.model.usagedata.AppSession
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import java.util.Calendar
import kotlin.collections.iterator
import kotlin.math.max

/**
 * Repository for reading local app-usage data from Android UsageStats / UsageEvents.
 *
 * Responsibilities:
 * 1. Implement [LocalUsageRepo] for the domain layer.
 * 2. Reconstruct recent tracked-app sessions from UsageEvents.
 * 3. Provide daily usage summaries for UI/statistics screens.
 * 4. Provide Android permission and settings helpers.
 *
 * Notes:
 * - Session-based intervention logic should use [getRecentSessions].
 * - Daily total functions are kept because other parts of the app may still use them.
 * - This repository only returns data for tracked/enabled apps.
 */
class UsageStatsRepository(
    private val context: Context
) : LocalUsageRepo {

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val packageManager: PackageManager = context.packageManager

    // =========================================================
    // LocalUsageRepo implementation
    // =========================================================

    /**
     * Returns total tracked-app usage minutes for today.
     *
     * This is a daily aggregate helper required by [LocalUsageRepo].
     * It is not the new session-based trigger signal, but may still be used by
     * UI or compatibility paths.
     */
    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int {
        return getTodayUsageStats(enabled).sumOf { it.usageTimeMinutes }
    }

    /**
     * Returns daily total usage minutes for the previous 7 days, excluding today.
     *
     * Current behavior:
     * - query 8 day buckets in total
     * - drop index 0 (today)
     * - keep the next 7 entries
     */
    override suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int> {
        return getUsageStatsForDays(days = 8, enabled = enabled)
            .drop(1)
            .take(7)
            .map { it.totalMinutes }
    }

    /**
     * Returns reconstructed tracked-app sessions within the last [days] days.
     *
     * This is the key entry point for the new session-based intervention logic.
     * Each returned [AppSession] represents one continuous foreground session
     * for a tracked app.
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

    // =========================================================
    // Session reconstruction
    // =========================================================

    /**
     * Reconstructs tracked-app sessions from UsageEvents over the past [days] days.
     *
     * A session starts when a tracked app moves to foreground/resumed,
     * and ends when it moves to background/paused/stopped, or when the screen
     * becomes non-interactive / keyguard is shown.
     *
     * Rules:
     * - only tracked apps inside [enabledPackages] are included
     * - sessions shorter than 1 minute are discarded
     * - any unfinished session at the end of the query window is closed at [end]
     *
     * This function is the underlying data source used by [getRecentSessions].
     */
    fun getSessionsForDays(
        days: Int,
        enabledPackages: Set<String>
    ): List<AppSession> {
        if (days <= 0) return emptyList()

        val start = startOfDayMillis(daysAgo = days - 1)
        val end = System.currentTimeMillis()

        val enabledMetas = TrackedApps.all.filter { meta ->
            meta.exactPackages.any { it in enabledPackages }
        }
        if (enabledMetas.isEmpty()) return emptyList()

        // Exact mapping from package name to tracked app id.
        val exactPkgToId: Map<String, TrackedAppId> =
            enabledMetas
                .flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }
                .toMap()

        /**
         * Resolves a raw package name to a tracked app id.
         *
         * Resolution order:
         * 1. exact package match
         * 2. fallback substring match
         */
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

        /**
         * Closes the currently open tracked-app session at time [at].
         *
         * Sessions shorter than 60 seconds are ignored.
         */
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
                        // If another tracked app session is already open, close it first.
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

        // Close any tracked app session still open at the end of the query window.
        if (currentPkg != null && currentStart != null) {
            closeCurrent(end)
        }

        return sessions
    }

    // =========================================================
    // Permission and settings helpers
    // =========================================================

    /**
     * Returns true if the app has Usage Access permission.
     *
     * This uses AppOpsManager, which is more reliable than checking UI state.
     */
    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Opens the Android Usage Access settings screen.
     */
    fun openUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        context.startActivity(intent)
    }

    /**
     * Returns true if this app's accessibility service is enabled.
     *
     * This checks both the full component name and shorthand format because
     * Android may store either representation.
     */
    fun hasAccessibilityPermission(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        Log.d("UsageStatsRepo", "Accessibility enabled setting: $accessibilityEnabled")

        if (accessibilityEnabled != 1) {
            Log.d("UsageStatsRepo", "Accessibility is globally disabled")
            return false
        }

        val serviceFullName =
            "${context.packageName}/${context.packageName}.blocker.AccessibilityMonitorService"
        val serviceShorthand =
            "${context.packageName}/.blocker.AccessibilityMonitorService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        Log.d("UsageStatsRepo", "Looking for service: $serviceFullName or $serviceShorthand")
        Log.d("UsageStatsRepo", "Enabled services: $enabledServices")

        if (enabledServices == null) {
            Log.d("UsageStatsRepo", "No enabled services found")
            return false
        }

        val result = enabledServices.contains(serviceFullName) ||
                enabledServices.contains(serviceShorthand)

        Log.d("UsageStatsRepo", "Service found: $result")
        return result
    }

    /**
     * Opens the Android Accessibility settings screen.
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // =========================================================
    // Daily usage summary helpers
    // =========================================================

    /**
     * Returns per-app usage statistics for today only.
     *
     * This is mainly useful for UI display and daily summaries.
     * It is not the primary signal for the new session-based intervention logic.
     */
    fun getTodayUsageStats(
        enabled: Set<TrackedAppId> = TrackedApps.allIds
    ): List<AppUsageInfo> {
        val start = startOfDayMillis(daysAgo = 0)
        val end = System.currentTimeMillis()
        return getUsageStats(start, end, enabled)
    }

    /**
     * Returns daily usage summaries for the last [days] day buckets.
     *
     * Output ordering:
     * - index 0 = today
     * - index 1 = yesterday
     * - ...
     *
     * Each [DailyUsageInfo] contains:
     * - the day start timestamp
     * - total usage minutes for that day
     * - per-app usage details
     */
    fun getUsageStatsForDays(
        days: Int,
        enabled: Set<TrackedAppId> = TrackedApps.allIds
    ): List<DailyUsageInfo> {
        val now = System.currentTimeMillis()
        val result = mutableListOf<DailyUsageInfo>()

        for (i in 0 until days) {
            val start = startOfDayMillis(daysAgo = i)
            val end = minOf(endOfDayMillis(daysAgo = i), now)

            val apps = getUsageStats(start, end, enabled)
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
     * Computes per-app usage statistics over a given time range.
     *
     * This reconstructs foreground usage from UsageEvents and then aggregates
     * matching packages into tracked apps.
     *
     * Output characteristics:
     * - only enabled tracked apps are included
     * - apps are seeded with 0 minutes for stable UI rendering
     * - package variants can be aggregated into one tracked app
     */
    private fun getUsageStats(
        startTime: Long,
        endTime: Long,
        enabled: Set<TrackedAppId>
    ): List<AppUsageInfo> {
        if (endTime <= startTime) return emptyList()

        val enabledMetas = TrackedApps.all.filter { it.id in enabled }
        if (enabledMetas.isEmpty()) return emptyList()

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

        val events: UsageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        val totalMs = mutableMapOf<String, Long>()
        val lastUsed = mutableMapOf<String, Long>()

        var currentPkg: String? = null
        var currentStart: Long? = null

        fun addTime(pkg: String, from: Long, to: Long) {
            val delta = (to - from).coerceAtLeast(0L)
            if (delta <= 0L) return
            totalMs[pkg] = (totalMs[pkg] ?: 0L) + delta
        }

        fun closeCurrent(at: Long) {
            val pkg = currentPkg
            val start = currentStart
            if (pkg != null && start != null) {
                addTime(pkg, start, at)
            }
            currentPkg = null
            currentStart = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val pkg = event.packageName ?: continue
            val t = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (currentPkg != null && currentStart != null && currentPkg != pkg) {
                        closeCurrent(t)
                    }
                    currentPkg = pkg
                    currentStart = max(t, startTime)
                    lastUsed[pkg] = max(lastUsed[pkg] ?: 0L, t)
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentPkg == pkg && currentStart != null) {
                        closeCurrent(t)
                    }
                    lastUsed[pkg] = max(lastUsed[pkg] ?: 0L, t)
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    closeCurrent(t)
                }
            }
        }

        if (currentPkg != null && currentStart != null) {
            closeCurrent(endTime)
        }

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

        for ((pkg, ms) in totalMs) {
            if (ms <= 0L) continue
            if (!isAppLaunchable(pkg)) continue

            val id = resolveTrackedId(pkg) ?: continue
            val meta = TrackedApps.metaFor(id)

            val minutes = (ms / 60_000L).toInt()
            val usedAt = lastUsed[pkg] ?: 0L

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

        return installedEnabledMetas
            .mapNotNull { resultsById[it.id] }
            .sortedByDescending { it.usageTimeMinutes }
    }

    // =========================================================
    // Internal helpers
    // =========================================================

    /**
     * Returns true if the package appears to be a launchable user-facing app.
     */
    private fun isAppLaunchable(packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        } catch (_: Exception) {
            false
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
     * Returns true if any package belonging to this tracked app is installed.
     */
    private fun isTrackedAppInstalled(meta: TrackedAppMeta): Boolean {
        return meta.exactPackages.any { isAppInstalled(it) }
    }

    /**
     * Returns the local start-of-day timestamp for [daysAgo].
     *
     * Examples:
     * - 0 = today at 00:00:00.000
     * - 1 = yesterday at 00:00:00.000
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
     * Returns the local end-of-day timestamp for [daysAgo].
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

/**
 * Per-app usage summary used by UI and daily statistics screens.
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val lastTimeUsed: Long,
    val category: AppCategory
)

/**
 * Daily usage summary containing total minutes and per-app breakdown for one day.
 */
data class DailyUsageInfo(
    val date: Long,
    val totalMinutes: Int,
    val apps: List<AppUsageInfo>
)

/**
 * Simple app category enum used by UI filtering/grouping.
 */
enum class AppCategory {
    SOCIAL,
    ENTERTAINMENT,
    PRODUCTIVITY,
    GAMES,
    OTHER
}