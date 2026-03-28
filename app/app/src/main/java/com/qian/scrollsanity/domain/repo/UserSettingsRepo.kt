package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.model.user.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserSettingsRepo {
    fun observeUserPreferences(): Flow<UserPreferences>
    suspend fun getUserPreferences(): UserPreferences

    suspend fun updateInterventionIntensity(intensity: String)
    suspend fun updateToneStyle(style: String)
    suspend fun updateDailyGoalMinutes(minutes: Int)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateFocusStrictMode(enabled: Boolean)
    suspend fun updateDarkMode(mode: String)
    suspend fun updateEnabledTrackedApps(ids: Set<TrackedAppId>)
    suspend fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean)

    suspend fun syncFromRemote(): Boolean
    suspend fun syncToRemote(): Boolean
}