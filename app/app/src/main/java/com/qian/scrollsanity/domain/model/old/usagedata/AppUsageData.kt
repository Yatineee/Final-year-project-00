package com.qian.scrollsanity.domain.model.old.usagedata

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore model for per-app usage tracking
 * Structure: users/{userId}/apps/{appId}
 * Matches Chrome extension format
 */
data class AppUsageData(
    @DocumentId
    val appId: String = "",
    val date: String = "", // ISO format: "yyyy-MM-dd"
    val secondsToday: Long = 0,
    @ServerTimestamp
    val lastUpdated: Date? = null
)