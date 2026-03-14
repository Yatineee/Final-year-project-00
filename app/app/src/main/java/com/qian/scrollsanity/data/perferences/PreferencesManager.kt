package com.qian.scrollsanity.data.perferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "arete_settings")

class PreferencesManager(private val context: Context) {

    private val firestoreRepo = FirestoreRepository()
    private val firebaseAuth = FirebaseAuth.getInstance()

    private object PreferencesKeys {
        // ===== Existing local settings =====
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val FOCUS_STRICT_MODE = booleanPreferencesKey("focus_strict_mode")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        // Enabled tracked apps (store enum names)
        val ENABLED_TRACKED_APPS = stringSetPreferencesKey("enabled_tracked_apps")

        // Focus session state
        val IS_FOCUS_ACTIVE = booleanPreferencesKey("is_focus_active")
        val FOCUS_END_TIME = longPreferencesKey("focus_end_time")

        // Auth state
        val IS_USER_LOGGED_IN = booleanPreferencesKey("is_user_logged_in")

        // ===== NEW: Intervention / language generation preferences =====
        val INTERVENTION_INTENSITY = stringPreferencesKey("intervention_intensity") // "LOW"|"MEDIUM"|"HIGH"
        val TONE_STYLE = stringPreferencesKey("tone_style") // "gentle"|...
        val INTERESTS = stringSetPreferencesKey("interests") // store as set
        val RECENT_GOAL_CONTEXT = stringPreferencesKey("recent_goal_context") // optional string


    }

    // =====================================================
    // Existing Local Settings (kept)
    // =====================================================

    val dailyGoalMinutes: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 240 }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_GOAL_MINUTES] = minutes
        }
        syncPreferencesToFirestore()
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
        syncPreferencesToFirestore()
    }

    val focusStrictMode: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.FOCUS_STRICT_MODE] ?: false }

    suspend fun setFocusStrictMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.FOCUS_STRICT_MODE] = enabled
        }
        syncPreferencesToFirestore()
    }

    val darkMode: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.DARK_MODE] ?: "system" }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_MODE] = mode
        }
        // Dark mode change doesn’t have to hit Firestore unless you want it to.
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    // NEW: Enabled tracked apps
    val enabledTrackedApps: Flow<Set<TrackedAppId>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[PreferencesKeys.ENABLED_TRACKED_APPS] ?: TrackedApps.defaultEnabledIdStrings()
            raw.mapNotNull { s -> runCatching { TrackedAppId.valueOf(s) }.getOrNull() }
                .toSet()
                .ifEmpty { TrackedApps.allIds }
        }

    suspend fun setEnabledTrackedApps(ids: Set<TrackedAppId>) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ENABLED_TRACKED_APPS] = ids.map { it.name }.toSet()
        }
        syncPreferencesToFirestore()
    }

    suspend fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = (prefs[PreferencesKeys.ENABLED_TRACKED_APPS] ?: TrackedApps.defaultEnabledIdStrings())
                .toMutableSet()

            if (enabled) current.add(id.name) else current.remove(id.name)

            if (current.isEmpty()) current.addAll(TrackedApps.defaultEnabledIdStrings())

            prefs[PreferencesKeys.ENABLED_TRACKED_APPS] = current
        }
        syncPreferencesToFirestore()
    }

    // Focus Session State
    val isFocusActive: Flow<Boolean> = context.dataStore.data
        .map { prefs ->
            val isActive = prefs[PreferencesKeys.IS_FOCUS_ACTIVE] ?: false
            val endTime = prefs[PreferencesKeys.FOCUS_END_TIME] ?: 0L
            if (isActive && endTime > 0 && System.currentTimeMillis() > endTime) false else isActive
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

    // Auth state
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
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    // =====================================================
    // NEW: Intervention Preferences (DataStore + Firestore sync)
    // =====================================================

    /** "LOW" | "MEDIUM" | "HIGH" */
    val interventionIntensity: Flow<String> = context.dataStore.data
        .map { prefs ->
            val raw = (prefs[PreferencesKeys.INTERVENTION_INTENSITY] ?: "MEDIUM").uppercase()
            when (raw) {
                "LOW", "MEDIUM", "HIGH" -> raw
                else -> "MEDIUM"
            }
        }

    suspend fun setInterventionIntensity(intensity: String, syncCloud: Boolean = true) {
        val normalized = intensity.uppercase()
        val safe = when (normalized) {
            "LOW", "MEDIUM", "HIGH" -> normalized
            else -> "MEDIUM"
        }

        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.INTERVENTION_INTENSITY] = safe
        }

        if (syncCloud) {
            syncPreferencesToFirestore()
        }
    }

    /** e.g. "gentle", "humorous", "direct" */
    val toneStyle: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[PreferencesKeys.TONE_STYLE] ?: "gentle" }

    suspend fun setToneStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.TONE_STYLE] = style
        }
        syncPreferencesToFirestore()
    }

    /** Interests stored as a set locally; emitted as a sorted list for stable UI */
    val interests: Flow<List<String>> = context.dataStore.data
        .map { prefs ->
            val set = prefs[PreferencesKeys.INTERESTS] ?: emptySet()
            set.map { it.trim() }.filter { it.isNotBlank() }.sorted()
        }

    suspend fun setInterests(items: List<String>) {
        val cleaned = items.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.INTERESTS] = cleaned
        }
        syncPreferencesToFirestore()
    }

    /** Recent goal context like "prepare IELTS" */
    val recentGoalContext: Flow<String?> = context.dataStore.data
        .map { prefs ->
            prefs[PreferencesKeys.RECENT_GOAL_CONTEXT]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

    suspend fun setRecentGoalContext(text: String?) {
        val cleaned = text?.trim()?.takeIf { it.isNotBlank() }
        context.dataStore.edit { prefs ->
            if (cleaned == null) prefs.remove(PreferencesKeys.RECENT_GOAL_CONTEXT)
            else prefs[PreferencesKeys.RECENT_GOAL_CONTEXT] = cleaned
        }
        syncPreferencesToFirestore()
    }

    // =====================================================
    // FIRESTORE SYNC (Updated)
    // =====================================================

    /**
     * Sync current local preferences (DataStore) to Firestore
     *
     * IMPORTANT: This should NOT upload daily usage events; only preference fields.
     */
    private suspend fun syncPreferencesToFirestore() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        try {
            val prefs = context.dataStore.data.first()

            val userPreferences = UserPreferences(
                // NEW fields
                interventionIntensity = (prefs[PreferencesKeys.INTERVENTION_INTENSITY] ?: "MEDIUM").uppercase(),
                toneStyle = prefs[PreferencesKeys.TONE_STYLE] ?: "gentle",
                interests = (prefs[PreferencesKeys.INTERESTS] ?: emptySet()).toList(),
                recentGoalContext = prefs[PreferencesKeys.RECENT_GOAL_CONTEXT],

                // Existing local settings (still synced, since you were doing it)
                dailyGoalMinutes = prefs[PreferencesKeys.DAILY_GOAL_MINUTES] ?: 240,
                notificationsEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
                focusStrictMode = prefs[PreferencesKeys.FOCUS_STRICT_MODE] ?: false,
                darkMode = prefs[PreferencesKeys.DARK_MODE] ?: "system",
                enabledTrackedApps = (prefs[PreferencesKeys.ENABLED_TRACKED_APPS]
                    ?: TrackedApps.defaultEnabledIdStrings()).toList(),

                // Optional client updatedAt
                updatedAt = System.currentTimeMillis()
            )

            firestoreRepo.syncPreferences(userId, userPreferences)
        } catch (e: Exception) {
            Log.e("PreferencesManager", "Failed to sync preferences to Firestore", e)
        }
    }

    /**
     * Load preferences from Firestore (called on login)
     * - Pulls both NEW and existing settings into DataStore
     */
    suspend fun loadPreferencesFromFirestore(): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false

        return try {
            val result = firestoreRepo.getPreferences(userId)
            val firestorePrefs = result.getOrNull()

            if (firestorePrefs != null) {
                context.dataStore.edit { prefs ->
                    // NEW fields
                    prefs[PreferencesKeys.INTERVENTION_INTENSITY] =
                        firestorePrefs.interventionIntensity.ifBlank { "MEDIUM" }.uppercase()
                    prefs[PreferencesKeys.TONE_STYLE] =
                        firestorePrefs.toneStyle.ifBlank { "gentle" }
                    prefs[PreferencesKeys.INTERESTS] =
                        firestorePrefs.interests.map { it.trim() }.filter { it.isNotBlank() }.toSet()

                    firestorePrefs.recentGoalContext?.trim()?.takeIf { it.isNotBlank() }?.let {
                        prefs[PreferencesKeys.RECENT_GOAL_CONTEXT] = it
                    } ?: prefs.remove(PreferencesKeys.RECENT_GOAL_CONTEXT)

                    // Existing local settings
                    prefs[PreferencesKeys.DAILY_GOAL_MINUTES] = firestorePrefs.dailyGoalMinutes
                    prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = firestorePrefs.notificationsEnabled
                    prefs[PreferencesKeys.FOCUS_STRICT_MODE] = firestorePrefs.focusStrictMode
                    prefs[PreferencesKeys.DARK_MODE] = firestorePrefs.darkMode
                    prefs[PreferencesKeys.ENABLED_TRACKED_APPS] = firestorePrefs.enabledTrackedApps.toSet()
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
}