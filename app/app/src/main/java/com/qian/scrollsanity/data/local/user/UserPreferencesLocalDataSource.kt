package com.qian.scrollsanity.data.local.user

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.util.TrackedApps
import com.qian.scrollsanity.domain.model.user.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

class UserPreferencesLocalDataSource(
    private val context: Context
) {

    private object Keys {
        val INTERVENTION_INTENSITY = stringPreferencesKey("intervention_intensity")
        val TONE_STYLE = stringPreferencesKey("tone_style")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FOCUS_STRICT_MODE = booleanPreferencesKey("focus_strict_mode")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val ENABLED_TRACKED_APPS = stringSetPreferencesKey("enabled_tracked_apps")
    }

    fun observeUserPreferences(): Flow<UserPreferences> {
        return context.userPreferencesDataStore.data.map { prefs ->
            UserPreferences(
                interventionIntensity = normalizeIntensity(
                    prefs[Keys.INTERVENTION_INTENSITY] ?: "MEDIUM"
                ),
                toneStyle = prefs[Keys.TONE_STYLE]?.trim()?.ifBlank { "gentle" } ?: "gentle",
                dailyGoalMinutes = (prefs[Keys.DAILY_GOAL_MINUTES] ?: 240).coerceAtLeast(1),
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                focusStrictMode = prefs[Keys.FOCUS_STRICT_MODE] ?: false,
                darkMode = prefs[Keys.DARK_MODE] ?: "system",
                enabledTrackedApps = (
                        prefs[Keys.ENABLED_TRACKED_APPS]
                            ?: TrackedApps.defaultEnabledIdStrings()
                        ).toList()
            )
        }
    }

    suspend fun getUserPreferences(): UserPreferences {
        return observeUserPreferences().first()
    }

    suspend fun replaceAll(preferences: UserPreferences) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.INTERVENTION_INTENSITY] =
                normalizeIntensity(preferences.interventionIntensity)

            prefs[Keys.TONE_STYLE] =
                preferences.toneStyle.trim().ifBlank { "gentle" }

            prefs[Keys.DAILY_GOAL_MINUTES] =
                preferences.dailyGoalMinutes.coerceAtLeast(1)

            prefs[Keys.NOTIFICATIONS_ENABLED] =
                preferences.notificationsEnabled

            prefs[Keys.FOCUS_STRICT_MODE] =
                preferences.focusStrictMode

            prefs[Keys.DARK_MODE] =
                preferences.darkMode.ifBlank { "system" }

            prefs[Keys.ENABLED_TRACKED_APPS] =
                preferences.enabledTrackedApps.toSet().ifEmpty {
                    TrackedApps.defaultEnabledIdStrings()
                }
        }
    }

    suspend fun setInterventionIntensity(intensity: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.INTERVENTION_INTENSITY] = normalizeIntensity(intensity)
        }
    }

    suspend fun setToneStyle(style: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.TONE_STYLE] = style.trim().ifBlank { "gentle" }
        }
    }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.DAILY_GOAL_MINUTES] = minutes.coerceAtLeast(1)
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setFocusStrictMode(enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.FOCUS_STRICT_MODE] = enabled
        }
    }

    suspend fun setDarkMode(mode: String) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = mode.ifBlank { "system" }
        }
    }

    suspend fun setEnabledTrackedApps(ids: Set<TrackedAppId>) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[Keys.ENABLED_TRACKED_APPS] =
                ids.map { it.name }.toSet().ifEmpty {
                    TrackedApps.defaultEnabledIdStrings()
                }
        }
    }

    suspend fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            val current = (
                    prefs[Keys.ENABLED_TRACKED_APPS]
                        ?: TrackedApps.defaultEnabledIdStrings()
                    ).toMutableSet()

            if (enabled) {
                current.add(id.name)
            } else {
                current.remove(id.name)
            }

            if (current.isEmpty()) {
                current.addAll(TrackedApps.defaultEnabledIdStrings())
            }

            prefs[Keys.ENABLED_TRACKED_APPS] = current
        }
    }

    private fun normalizeIntensity(raw: String): String {
        return when (raw.trim().uppercase()) {
            "LOW", "MEDIUM", "HIGH" -> raw.trim().uppercase()
            else -> "MEDIUM"
        }
    }
}