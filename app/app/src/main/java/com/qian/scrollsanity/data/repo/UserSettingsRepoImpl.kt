package com.qian.scrollsanity.data.repo

import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.local.user.UserPreferencesLocalDataSource
import com.qian.scrollsanity.data.remote.user.UserPreferencesRemoteDataSource
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.repo.UserSettingsRepo
import kotlinx.coroutines.flow.Flow

class UserSettingsRepoImpl(
    private val localDataSource: UserPreferencesLocalDataSource,
    private val remoteDataSource: UserPreferencesRemoteDataSource,
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : UserSettingsRepo {

    override fun observeUserPreferences(): Flow<UserPreferences> {
        return localDataSource.observeUserPreferences()
    }

    override suspend fun getUserPreferences(): UserPreferences {
        return localDataSource.getUserPreferences()
    }

    override suspend fun updateInterventionIntensity(intensity: String) {
        localDataSource.setInterventionIntensity(intensity)
        syncToRemote()
    }

    override suspend fun updateToneStyle(style: String) {
        localDataSource.setToneStyle(style)
        syncToRemote()
    }

    override suspend fun updateDailyGoalMinutes(minutes: Int) {
        localDataSource.setDailyGoalMinutes(minutes)
        syncToRemote()
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        localDataSource.setNotificationsEnabled(enabled)
        syncToRemote()
    }

    override suspend fun updateFocusStrictMode(enabled: Boolean) {
        localDataSource.setFocusStrictMode(enabled)
        syncToRemote()
    }

    override suspend fun updateDarkMode(mode: String) {
        localDataSource.setDarkMode(mode)
        syncToRemote()
    }

    override suspend fun updateEnabledTrackedApps(ids: Set<TrackedAppId>) {
        localDataSource.setEnabledTrackedApps(ids)
        syncToRemote()
    }

    override suspend fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean) {
        localDataSource.setTrackedAppEnabled(id, enabled)
        syncToRemote()
    }

    override suspend fun syncFromRemote(): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        val remotePreferences = remoteDataSource.fetchUserPreferences(userId) ?: return false
        localDataSource.replaceAll(remotePreferences)
        return true
    }

    override suspend fun syncToRemote(): Boolean {
        val userId = firebaseAuth.currentUser?.uid ?: return false
        val localPreferences = localDataSource.getUserPreferences()
        remoteDataSource.saveUserPreferences(userId, localPreferences)
        return true
    }
}