package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateDarkModeUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(
        mode: String
    ) {
        repo.updateDarkMode(mode)
    }
}