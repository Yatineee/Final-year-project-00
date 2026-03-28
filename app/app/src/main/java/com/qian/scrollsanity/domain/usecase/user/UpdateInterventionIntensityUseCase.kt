package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateInterventionIntensityUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(intensity: String) {
        repo.updateInterventionIntensity(intensity)
    }
}