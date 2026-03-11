package com.qian.scrollsanity.data

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
import com.qian.scrollsanity.domain.model.AppSession
import java.util.Calendar
import kotlin.math.max


/**
 * UsageStatsRepository (per-app toggle ready)
 * 跟 Android 的 UsageStatsManager 对接
 * 检查 Usage Access permission
 * 查询 UsageEvents
 * 计算 app 使用时长
 * 返回今天、过去几天的 usage 数据
 * Android usage 数据采集器
 * 这也是我们接下来要加 session 逻辑的核心地方
 *
 *
 * - Reliable permission check via AppOpsManager
 * - Accurate-ish usage via UsageEvents foreground/background reconstruction
 * - Filters to ENABLED tracked apps only (from Preferences)
 * - Returns stable UI entries (0m entries) for enabled apps
 * - Aggregates variants (multiple packages) into a single tracked app if needed
 */


class UsageStatsRepository(private val context: Context) {

    private val usageStatsManager: UsageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val packageManager: PackageManager = context.packageManager
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

        // Exact mapping: package -> tracked app id
        val exactPkgToId: Map<String, TrackedAppId> =
            enabledMetas.flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }.toMap()

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

                // Filter out very short sessions (< 60 seconds)
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

                    // Only record tracked apps
                    if (trackedId != null) {
                        // If another tracked app was active, close it first
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

        // Close any unfinished tracked app session at the end of the window
        if (currentPkg != null && currentStart != null) {
            closeCurrent(end)
        }

        return sessions
    }
//    fun getSessionsForDays(
//        days: Int,
//        enabled: Set<TrackedAppId>
//    ): List<AppSession> {
//
//        val start = startOfDayMillis(daysAgo = days - 1)
//        val end = System.currentTimeMillis()
//
//        val enabledMetas = TrackedApps.all.filter { it.id in enabled }
//
//        val exactPkgToId: Map<String, TrackedAppId> =
//            enabledMetas.flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }.toMap()
//
//        fun resolveTrackedId(packageName: String): TrackedAppId? {
//            exactPkgToId[packageName]?.let { return it }
//            val lower = packageName.lowercase()
//            for (meta in enabledMetas) {
//                if (meta.fallbackContains.any { lower.contains(it) }) return meta.id
//            }
//            return null
//        }
//
//        val events: UsageEvents = usageStatsManager.queryEvents(start, end)
//        val event = UsageEvents.Event()
//
//        val sessions = mutableListOf<AppSession>()
//
//        var currentPkg: String? = null
//        var currentStart: Long? = null
//        var currentTrackedId: TrackedAppId? = null
//
//        fun closeCurrent(at: Long) {
//            val pkg = currentPkg
//            val startTime = currentStart
//            val trackedId = currentTrackedId
//
//            if (pkg != null && startTime != null && trackedId != null) {
//
//                val durationMillis = (at - startTime).coerceAtLeast(0L)
//                val durationMinutes = (durationMillis / 60_000L).toInt()
//
//                if (durationMinutes > 0) {
//                    sessions.add(
//                        AppSession(
//                            trackedAppId = trackedId,
//                            packageName = pkg,
//                            startMillis = startTime,
//                            endMillis = at,
//                            durationMinutes = durationMinutes
//                        )
//                    )
//                }
//            }
//
//            currentPkg = null
//            currentStart = null
//            currentTrackedId = null
//        }
//
//        while (events.hasNextEvent()) {
//            events.getNextEvent(event)
//
//            val pkg = event.packageName ?: continue
//            val t = event.timeStamp
//
//            when (event.eventType) {
//
//                UsageEvents.Event.MOVE_TO_FOREGROUND,
//                UsageEvents.Event.ACTIVITY_RESUMED -> {
//
//                    val trackedId = resolveTrackedId(pkg)
//
//                    if (trackedId != null) {
//
//                        if (currentPkg != null && currentPkg != pkg) {
//                            closeCurrent(t)
//                        }
//
//                        currentPkg = pkg
//                        currentStart = max(t, start)
//                        currentTrackedId = trackedId
//                    }
//                }
//
//                UsageEvents.Event.MOVE_TO_BACKGROUND,
//                UsageEvents.Event.ACTIVITY_PAUSED,
//                UsageEvents.Event.ACTIVITY_STOPPED -> {
//
//                    if (currentPkg == pkg && currentStart != null) {
//                        closeCurrent(t)
//                    }
//                }
//
//                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
//                UsageEvents.Event.KEYGUARD_SHOWN -> {
//                    closeCurrent(t)
//                }
//            }
//        }
//
//        if (currentPkg != null && currentStart != null) {
//            closeCurrent(end)
//        }
//
//        return sessions
//    }
    /**
     * Reliable check for Usage Access permission.
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
     * Open system settings to grant usage access permission.
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
     * Check if accessibility service is enabled for this app.
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

        // Android registers with full class name, not shorthand
        val serviceFullName = "${context.packageName}/${context.packageName}.blocker.AccessibilityMonitorService"
        val serviceShorthand = "${context.packageName}/.blocker.AccessibilityMonitorService"

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

        // Check both formats since Android may use either
        val result = enabledServices.contains(serviceFullName) || enabledServices.contains(serviceShorthand)
        Log.d("UsageStatsRepo", "Service found: $result")
        return result
    }

    /**
     * Open accessibility settings.
     */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Get today's usage statistics (from local midnight to now).
     * Returns ONLY enabled tracked apps.
     */
    fun getTodayUsageStats(enabled: Set<TrackedAppId> = TrackedApps.allIds): List<AppUsageInfo> {
        val start = startOfDayMillis(daysAgo = 0)
        val end = System.currentTimeMillis()
        return getUsageStats(start, end, enabled)
    }

    /**
     * Get usage statistics for the past N days.
     * Returns one DailyUsageInfo per day (0 = today, 1 = yesterday, ...).
     * Each day contains ONLY enabled tracked apps.
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
     * Accurate usage computation for a time range using UsageEvents.
     *
     * Robust approach:
     * - Track a single "current foreground app" to reduce overcounting
     * - Close session on screen non-interactive / keyguard shown (if present)
     *
     * Output:
     * - Only enabled tracked apps
     * - Seed with 0m entries for enabled apps for stable UI
     */
    private fun getUsageStats(
        startTime: Long,
        endTime: Long,
        enabled: Set<TrackedAppId>
    ): List<AppUsageInfo> {
        if (endTime <= startTime) return emptyList()

        val enabledMetas = TrackedApps.all.filter { it.id in enabled }

        // If user somehow disables everything, return empty (PreferencesManager can prevent this)
        if (enabledMetas.isEmpty()) return emptyList()

        // Exact mapping for enabled apps: package -> tracked id
        val exactPkgToId: Map<String, TrackedAppId> =
            enabledMetas.flatMap { meta -> meta.exactPackages.map { pkg -> pkg to meta.id } }.toMap()

        fun resolveTrackedId(packageName: String): TrackedAppId? {
            exactPkgToId[packageName]?.let { return it }
            val lower = packageName.lowercase()
            for (meta in enabledMetas) {
                if (meta.fallbackContains.any { lower.contains(it) }) return meta.id
            }
            return null
        }

        val events: UsageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        val totalMs = mutableMapOf<String, Long>()   // package -> total foreground ms
        val lastUsed = mutableMapOf<String, Long>()  // package -> last seen timestamp

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
                    // Close previous if a different app comes to foreground
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

                // If your minSdk complains about these constants, delete these two cases.
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    closeCurrent(t)
                }
            }
        }

        // Close any app still in foreground at endTime
        if (currentPkg != null && currentStart != null) {
            closeCurrent(endTime)
        }

        // Seed output with enabled apps at 0 minutes (stable UI)
        // Only include apps that are actually installed on the device
        val installedEnabledMetas = enabledMetas.filter { isTrackedAppInstalled(it) }

        val resultsById: MutableMap<TrackedAppId, AppUsageInfo> =
            installedEnabledMetas.associate { meta ->
                val representativePkg = meta.exactPackages.firstOrNull() ?: meta.displayName.lowercase()
                meta.id to AppUsageInfo(
                    packageName = representativePkg,
                    appName = meta.displayName,
                    usageTimeMinutes = 0,
                    lastTimeUsed = 0L,
                    category = AppCategory.SOCIAL
                )
            }.toMutableMap()

        // Aggregate matching packages into the tracked app
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
                    // prefer a real package name if we had a placeholder
                    packageName = if (existing.packageName.startsWith("com.")) existing.packageName else pkg,
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

        // Return installed enabled apps in stable order, sorted by usage descending for UI
        return installedEnabledMetas
            .mapNotNull { resultsById[it.id] }
            .sortedByDescending { it.usageTimeMinutes }
    }

    /**
     * Helper to check if an app has a launcher activity (is a "real" user app).
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
     * Helper to check if an app is installed on the device.
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
     * Helper to check if any of the tracked app's packages are installed.
     */
    private fun isTrackedAppInstalled(meta: TrackedAppMeta): Boolean {
        return meta.exactPackages.any { isAppInstalled(it) }
    }

    /**
     * Calendar-based day boundaries.
     * daysAgo=0 => today, 1 => yesterday, etc.
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

    private fun endOfDayMillis(daysAgo: Int): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    /**
     * Get usage in minutes for a specific package today.
     */
    suspend fun getUsageMinutesTodayForPackage(packageName: String): Int {
        val start = startOfDayMillis(daysAgo = 0)
        val end = System.currentTimeMillis()

        if (!hasUsagePermission()) return 0

        val events: UsageEvents = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()

        var totalMs = 0L
        var currentStart: Long? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            if (event.packageName != packageName) continue

            val t = event.timeStamp

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND,
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    currentStart = max(t, start)
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentStart != null) {
                        val delta = (t - currentStart).coerceAtLeast(0L)
                        totalMs += delta
                        currentStart = null
                    }
                }

                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    if (currentStart != null) {
                        val delta = (t - currentStart).coerceAtLeast(0L)
                        totalMs += delta
                        currentStart = null
                    }
                }
            }
        }

        // If app is still in foreground at end time
        if (currentStart != null) {
            val delta = (end - currentStart).coerceAtLeast(0L)
            totalMs += delta
        }

        return (totalMs / 60_000L).toInt()
    }
}

/**
 * Data class representing usage info for a single app
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val lastTimeUsed: Long,
    val category: AppCategory
)

/**
 * Data class representing daily usage summary
 */
data class DailyUsageInfo(
    val date: Long,
    val totalMinutes: Int,
    val apps: List<AppUsageInfo>
)

/**
 * App categories for filtering
 */
enum class AppCategory {
    SOCIAL,
    ENTERTAINMENT,
    PRODUCTIVITY,
    GAMES,
    OTHER
}