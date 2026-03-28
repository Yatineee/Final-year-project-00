package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.user.UserProfile
import com.qian.scrollsanity.domain.repo.UserProfileRepo
import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase(
    private val repo: UserProfileRepo
) {
    operator fun invoke(userId: String): Flow<UserProfile?> = repo.observeUserProfile(userId)
}