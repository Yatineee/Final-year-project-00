package com.qian.scrollsanity.domain.model

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

    // =========================
    // NEW (preferred)
    // =========================
    val interventionIntensity: String = "MEDIUM",
    val toneStyle: String = "gentle",
    val interests: List<String> = emptyList(),
    val recentGoalContext: String? = null,

    // =========================
    // LEGACY (temporary)
    // =========================
    // Old logic / naming — keep for now to avoid breaking existing screens
    val habit: String = "",
    val goal: String = "",
    val recentChallenge: String = "",
    val preferStyle: String = "gentle",

    // =========================
    // EXISTING LOCAL SETTINGS
    // =========================
    val dailyGoalMinutes: Int = 240,
    val notificationsEnabled: Boolean = true,
    val focusStrictMode: Boolean = false,
    val darkMode: String = "system",
    val enabledTrackedApps: List<String> = emptyList(),

    @ServerTimestamp
    val lastSyncedAt: Date? = null,

    // Optional client-side updatedAt (matches repo writes)
    val updatedAt: Long? = null
)