package com.qian.scrollsanity.domain.repo

interface UsageRepo {
    /**
     * The use case calls this for "today's minutes across tracked apps".
     * Implementation can sum per-package usage.
     */
    suspend fun getTotalUsageTodayMinutes(trackedPackages: Set<String>): Int
}
