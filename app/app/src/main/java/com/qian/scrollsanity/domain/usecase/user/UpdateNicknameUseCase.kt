package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserProfileRepo

class UpdateNicknameUseCase(
    private val repo: UserProfileRepo
) {
    suspend operator fun invoke(userId: String, nickname: String) {
        repo.updateNickname(userId, nickname)
    }
}