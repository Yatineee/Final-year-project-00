package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateToneStyleUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(
        style: String
    ) {
        repo.updateToneStyle(style)
    }
}