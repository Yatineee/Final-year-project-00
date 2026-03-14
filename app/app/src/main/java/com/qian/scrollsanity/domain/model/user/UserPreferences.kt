package com.qian.scrollsanity.domain.model.user

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * UserPreferences (Firestore: users/{uid}/data/preferences)
 *
 * ✅ New fields (ScrollSanity design):
 * - interventionIntensity: "LOW" | "MEDIUM" | "HIGH"
 * - toneStyle: e.g. "gentle", "humorous", "direct"
 * - interests: List<String>
 * - recentGoalContext: user's recent goal (e.g. "prepare an exam")
 *
 * ⚠️ Legacy fields are kept temporarily to avoid breaking existing code paths.
 * You can remove them once UI/PreferencesManager migration is complete.
 */
data class UserPreferences(
    val interventionIntensity: String = "MEDIUM",
    val toneStyle: String = "gentle",
    val recentGoalContext: String? = null,
    val recentInterestContext: String? = null,

    val dailyGoalMinutes: Int = 240,
    val notificationsEnabled: Boolean = true,
    val focusStrictMode: Boolean = false,
    val darkMode: String = "system",
    val enabledTrackedApps: List<String> = emptyList(),

    @ServerTimestamp
    val lastSyncedAt: Date? = null,
    val updatedAt: Long? = null
)