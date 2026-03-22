package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.domain.model.usagedata.AppSession

interface LocalUsageRepo {
    suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int
    suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int>

    /**
     * Returns tracked app sessions within the recent N days.
     */
    suspend fun getRecentSessions(
        enabledPackages: Set<String>,
        days: Int
    ): List<AppSession>
}