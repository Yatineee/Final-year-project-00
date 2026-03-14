package com.qian.scrollsanity.data

import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.usecase.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.IntensityProvider

import kotlinx.coroutines.flow.first

class EnabledTrackedProviderImpl(
    private val prefs: PreferencesManager
) : EnabledTrackedProvider {
    override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
        return prefs.enabledTrackedApps.first()
    }
}

class IntensityProviderImpl(
    private val prefs: PreferencesManager
) : IntensityProvider {
    override suspend fun getIntensity(): String {
        // PreferencesManager 里你现在是 "LOW"|"MEDIUM"|"HIGH"
        return runCatching { prefs.interventionIntensity.first() }
            .getOrElse { "MEDIUM" }
    }
}

/**
 * Adapter: local usage from UsageStatsRepository -> domain LocalUsageProvider
 */
//class LocalUsageProviderImpl(
//    private val usageRepo: UsageStatsRepository
//) : LocalUsageProvider {
//
//    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int {
//        return usageRepo.getTodayUsageStats(enabled).sumOf { it.usageTimeMinutes }
//    }
//
//    /**
//     * Baseline = previous 7 days totals (exclude today).
//     * We request 8 days and drop index 0 (today).
//     */
//    override suspend fun getBaseline7DaysMinutes(enabled: Set<TrackedAppId>): List<Int> {
//        val days8 = usageRepo.getUsageStatsForDays(days = 8, enabled = enabled)
//            .map { it.totalMinutes }
//
//        // days8[0] is today, drop it -> yesterday..7 days ago
//        return if (days8.size >= 2) days8.drop(1).take(7) else emptyList()
//    }
//}
