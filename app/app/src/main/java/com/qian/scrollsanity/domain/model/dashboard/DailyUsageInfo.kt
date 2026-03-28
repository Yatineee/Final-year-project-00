package com.qian.scrollsanity.domain.model.dashboard

data class DailyUsageInfo(
    val date: Long,
    val totalMinutes: Int,
    val apps: List<AppUsageInfo>
)