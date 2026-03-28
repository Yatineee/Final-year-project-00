package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class GetUserPreferencesUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(): UserPreferences = repo.getUserPreferences()
}