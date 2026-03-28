package com.qian.scrollsanity.domain.model.dashboard

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val lastTimeUsed: Long,
    val category: AppCategory
)