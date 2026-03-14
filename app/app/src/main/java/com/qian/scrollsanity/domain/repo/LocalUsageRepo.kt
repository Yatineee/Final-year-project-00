package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.data.TrackedAppId
import com.qian.scrollsanity.domain.model.usagedata.AppSession

interface LocalUsageRepo {

    /**
     * Total usage minutes across enabled tracked apps today.
     */
    suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int

    /**
     * Total usage minutes for each of the last 7 days.
     * Index 0 = today, 1 = yesterday, ...
     */
    suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int>

    /**
     * Returns all tracked app sessions within the past N days.
     *
     * Each session represents one continuous foreground usage
     * of a tracked app.
     */
    suspend fun getRecentSessions(
        enabledPackages: Set<String>,
        days: Int
    ): List<AppSession>
}