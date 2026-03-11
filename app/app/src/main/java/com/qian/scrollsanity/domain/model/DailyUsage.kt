package com.qian.scrollsanity.domain.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class DailyUsage(
    @DocumentId
    val date: String = "", // Format: "yyyy-MM-dd"
    val totalMinutes: Int = 0,
    val apps: List<AppUsage> = emptyList(),
    @ServerTimestamp
    val lastUpdatedAt: Date? = null
)

data class AppUsage(
    val packageName: String = "",
    val appName: String = "",
    val usageMinutes: Int = 0
)
