package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateFocusStrictModeUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(
        enabled: Boolean
    ) {
        repo.updateFocusStrictMode(enabled)
    }
}