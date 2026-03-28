package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class UpdateEnabledTrackedAppsUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(
        ids: Set<TrackedAppId>
    ) {
        repo.updateEnabledTrackedApps(ids)
    }
}