package com.qian.scrollsanity.domain.model.user

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * UserPreferences (Firestore: users/{uid}/data/preferences)
 *
 * Primary fields:
 * - interventionIntensity
 * - toneStyle
 * - dailyGoalMinutes
 * - notificationsEnabled
 * - focusStrictMode
 * - darkMode
 * - enabledTrackedApps
 *
 * Notes:
 * - Goals and interests are now managed through their own collections.
 * - This preferences model no longer stores recentGoalContext or
 *   recentInterestContext as primary app state.
 */
data class UserPreferences(
    val interventionIntensity: String = "MEDIUM",
    val toneStyle: String = "gentle",

    val dailyGoalMinutes: Int = 240,
    val notificationsEnabled: Boolean = true,
    val focusStrictMode: Boolean = false,
    val darkMode: String = "system",
    val enabledTrackedApps: List<String> = emptyList(),

    @ServerTimestamp
    val lastSyncedAt: Date? = null,
    val updatedAt: Long? = null
)