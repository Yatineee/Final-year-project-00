package com.qian.scrollsanity.data.repo

import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.UserProfile
import com.qian.scrollsanity.domain.repo.UserProfileRepo
import kotlinx.coroutines.flow.Flow

class UserProfileRepoImpl(
    private val firestoreRepo: FirestoreRepository
) : UserProfileRepo {

    override fun observeUserProfile(userId: String): Flow<UserProfile?> {
        return firestoreRepo.observeUserProfile(userId)
    }

    override suspend fun updateNickname(userId: String, nickname: String) {
        firestoreRepo.updateNickname(userId, nickname.trim())
    }

    override suspend fun updateDisplayName(userId: String, displayName: String) {
        firestoreRepo.updateDisplayName(userId, displayName.trim())
    }
}