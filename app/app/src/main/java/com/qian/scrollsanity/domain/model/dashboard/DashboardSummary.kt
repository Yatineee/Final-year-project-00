package com.qian.scrollsanity.domain.model.dashboard

data class DashboardSummary(
    val currentZScore: Double?,
    val interventionThreshold: Double?,
    val interventionsToday: Int,
    val trackedUsageTodayMinutes: Int
)