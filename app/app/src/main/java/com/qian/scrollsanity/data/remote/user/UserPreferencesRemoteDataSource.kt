package com.qian.scrollsanity.data.remote.user

import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.UserPreferences

class UserPreferencesRemoteDataSource(
    private val firestoreRepository: FirestoreRepository
) {

    suspend fun fetchUserPreferences(userId: String): UserPreferences? {
        val result = firestoreRepository.getPreferences(userId)
        return result.getOrNull()
    }

    suspend fun saveUserPreferences(
        userId: String,
        preferences: UserPreferences
    ) {
        firestoreRepository.syncPreferences(userId, preferences)
    }
}