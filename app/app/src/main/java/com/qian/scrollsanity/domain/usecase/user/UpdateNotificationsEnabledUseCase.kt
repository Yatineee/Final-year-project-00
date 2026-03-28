package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateNotificationsEnabledUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(enabled: Boolean) {
        repo.updateNotificationsEnabled(enabled)
    }
}