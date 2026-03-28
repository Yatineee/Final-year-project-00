package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.domain.model.user.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserProfileRepo {
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    suspend fun updateNickname(userId: String, nickname: String)
    suspend fun updateDisplayName(userId: String, displayName: String)
}