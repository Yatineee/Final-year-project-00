package com.qian.scrollsanity.domain.model.dashboard

import com.qian.scrollsanity.domain.model.old.dashboard.DailyAverageSessionPoint

data class LiveDashboardSummary(
    val currentZScore: Double? = null,
    val interventionThreshold: Double? = null,
    val interventionsToday: Int = 0,
    val trackedUsageTodayMinutes: Int = 0,
    val inTrackedSession: Boolean = false,
    val currentSessionMinutes: Int = 0,
    val dailyAverageSessionPoints: List<DailyAverageSessionPoint> = emptyList()
)