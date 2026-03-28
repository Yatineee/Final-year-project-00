package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateDailyGoalMinutesUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(
        minutes: Int
    ) {
        repo.updateDailyGoalMinutes(minutes)
    }
}