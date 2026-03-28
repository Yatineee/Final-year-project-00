package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserProfileRepo

class UpdateDisplayNameUseCase(
    private val repo: UserProfileRepo
) {
    suspend operator fun invoke(userId: String, displayName: String) {
        repo.updateDisplayName(userId, displayName)
    }
}