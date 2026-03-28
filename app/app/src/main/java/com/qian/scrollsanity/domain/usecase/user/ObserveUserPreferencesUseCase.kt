package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.repo.UserSettingsRepo
import kotlinx.coroutines.flow.Flow

class ObserveUserPreferencesUseCase(
    private val repo: UserSettingsRepo
) {
    operator fun invoke(): Flow<UserPreferences> = repo.observeUserPreferences()
}