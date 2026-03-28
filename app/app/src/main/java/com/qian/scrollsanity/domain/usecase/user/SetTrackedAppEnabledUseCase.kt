package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class SetTrackedAppEnabledUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(id: TrackedAppId, enabled: Boolean) {
        repo.setTrackedAppEnabled(id, enabled)
    }
}