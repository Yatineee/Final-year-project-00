package com.qian.scrollsanity.data

import com.qian.scrollsanity.domain.model.AppSession
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
class LocalUsageRepoImpl(
    private val usage: UsageStatsRepository
) : LocalUsageRepo {

    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int {
        return usage.getTodayUsageStats(enabled).sumOf { it.usageTimeMinutes }
    }

    override suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int> {
        val days = usage.getUsageStatsForDays(days = 7, enabled = enabled)
        return days.map { it.totalMinutes }
    }

    /**
     * Returns all tracked app sessions within the past N days.
     */
    override suspend fun getRecentSessions(
        enabledPackages: Set<String>,
        days: Int
    ): List<AppSession> {
        return usage.getSessionsForDays(days = days, enabledPackages = enabledPackages)
    }
}