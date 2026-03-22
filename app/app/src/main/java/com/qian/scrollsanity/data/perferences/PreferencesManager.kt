package com.qian.scrollsanity.data.perferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.domain.model.user.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arete_settings")

class PreferencesManager(private val context: Context) {

    private val firestoreRepo = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private object PreferencesKeys {
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FOCUS_STRICT_MODE = booleanPreferencesKey("focus_strict_mode")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val ENABLED_TRACKED_APPS = stringSetPreferencesKey("enabled_tracked_apps")
        val IS_FOCUS_ACTIVE = booleanPreferencesKey("is_focus_active")
        val FOCUS_END_TIME = longPreferencesKey("focus_end_time")
        val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")
        val INTERVENTION_INTENSITY = stringPreferencesKey("intervention_intensity")
        val TONE_STYLE = stringPreferencesKey("tone_style")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
        syncPreferencesToFirestore()
    }

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 240 }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        val safe = minutes.coerceAtLeast(1)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_GOAL_MINUTES] = safe
        }
        syncPreferencesToFirestore()
    }

    val enabledTrackedApps: Flow<Set<TrackedAppId>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[PreferencesKeys.ENABLED_TRACKED_APPS]
                ?: TrackedApps.defaultEnabledIdStrings()

            raw.mapNotNull { s ->
                runCatching { TrackedAppId.valueOf(s) }.getOrNull()
            }.toSet().ifEmpty {
                TrackedApps.allIds
            }
        }

    suspend fun setEnabledTrackedApps(ids: Set<TrackedAppId>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ENABLED_TRACKED_APPS] = ids.map { it.name }.toSet()
        }
        syncPreferencesToFirestore()
    }

    suspend fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (
                    prefs[PreferencesKeys.ENABLED_TRACKED_APPS]
                        ?: TrackedApps.defaultEnabledIdStrings()
                    ).toMutableSet()

            if (enabled) current.add(id.name) else current.remove(id.name)

            if (current.isEmpty()) {
                current.addAll(TrackedApps.defaultEnabledIdStrings())
            }

            prefs[PreferencesKeys.ENABLED_TRACKED_APPS] = current
        }
        syncPreferencesToFirestore()
    }

    val isFocusActive: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            val isActive = prefs[PreferencesKeys.IS_FOCUS_ACTIVE] ?: false
            val endTime = prefs[PreferencesKeys.FOCUS_END_TIME] ?: 0L
            if (isActive && endTime > 0 && System.currentTimeMillis() > endTime) {
                false
            } else {
                isActive
            }
        }

    val focusEndTime: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.FOCUS_END_TIME] ?: 0L }

    suspend fun startFocusSession(durationMinutes: Int) {
        val endTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_FOCUS_ACTIVE] = true
            prefs[PreferencesKeys.FOCUS_END_TIME] = endTime
        }
    }

    suspend fun endFocusSession() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_FOCUS_ACTIVE] = false
            prefs[PreferencesKeys.FOCUS_END_TIME] = 0L
        }
    }

    val isUserLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.IS_USER_LOGGED_IN] ?: false }

    suspend fun setUserLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_USER_LOGGED_IN] = loggedIn
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.IS_USER_LOGGED_IN] = false
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    val interventionIntensity: Flow<String> = context.dataStore.data
        .map { prefs ->
            val raw = (prefs[PreferencesKeys.INTERVENTION_INTENSITY] ?: "MEDIUM").uppercase()
            when (raw) {
                "LOW", "MEDIUM", "HIGH" -> raw
                else -> "MEDIUM"
            }
        }

    suspend fun setInterventionIntensity(intensity: String, syncCloud: Boolean = true) {
        val safe = when (intensity.uppercase()) {
            "LOW", "MEDIUM", "HIGH" -> intensity.uppercase()
            else -> "MEDIUM"
        }

        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.INTERVENTION_INTENSITY] = safe
        }

        if (syncCloud) {
            syncPreferencesToFirestore()
        }
    }

    val toneStyle: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[PreferencesKeys.TONE_STYLE]?.trim()?.ifBlank { "gentle" } ?: "gentle"
        }

    suspend fun setToneStyle(style: String) {
        val cleaned = style.trim().ifBlank { "gentle" }
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TONE_STYLE] = cleaned
        }
        syncPreferencesToFirestore()
    }

    private suspend fun syncPreferencesToFirestore() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        try {
            val prefs = context.dataStore.data.first()

            val userPreferences = UserPreferences(
                interventionIntensity = (prefs[PreferencesKeys.INTERVENTION_INTENSITY] ?: "MEDIUM").uppercase(),
                toneStyle = prefs[PreferencesKeys.TONE_STYLE] ?: "gentle",

                dailyGoalMinutes = prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 240,
                notificationsEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                focusStrictMode = prefs[PreferencesKeys.FOCUS_STRICT_MODE] ?: false,
                darkMode = prefs[PreferencesKeys.DARK_MODE] ?: "system",
                enabledTrackedApps = (
                        prefs[PreferencesKeys.ENABLED_TRACKED_APPS]
                            ?: TrackedApps.defaultEnabledIdStrings()
                        ).toList(),

                updatedAt = System.currentTimeMillis()
            )

            firestoreRepo.syncPreferences(userId, userPreferences)
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Failed to sync preferences to Firestore", e)
        }
    }

    suspend fun loadPreferencesFromFirestore(): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false

        return try {
            val result = firestoreRepo.getPreferences(userId)
            val firestorePrefs = result.getOrNull()

            if (firestorePrefs != null) {
                context.dataStore.edit { prefs ->
                    prefs[PreferencesKeys.INTERVENTION_INTENSITY] =
                        firestorePrefs.interventionIntensity.ifBlank { "MEDIUM" }.uppercase()

                    prefs[PreferencesKeys.TONE_STYLE] =
                        firestorePrefs.toneStyle.ifBlank { "gentle" }

                    prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] =
                        firestorePrefs.notificationsEnabled

                    prefs[PreferencesKeys.FOCUS_STRICT_MODE] =
                        firestorePrefs.focusStrictMode

                    prefs[PreferencesKeys.DARK_MODE] =
                        firestorePrefs.darkMode

                    prefs[PreferencesKeys.DAILY_GOAL_MINUTES] =
                        firestorePrefs.dailyGoalMinutes

                    prefs[PreferencesKeys.ENABLED_TRACKED_APPS] =
                        firestorePrefs.enabledTrackedApps.toSet()
                }

                Log.d("PreferencesManager", "Preferences loaded from Firestore")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Failed to load preferences from Firestore", e)
            false
        }
    }

    val focusStrictMode: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.FOCUS_STRICT_MODE] ?: false }

    suspend fun setFocusStrictMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_STRICT_MODE] = enabled
        }
        syncPreferencesToFirestore()
    }
}