package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.UserSettingsRepo

class SyncUserPreferencesToRemoteUseCase(
    private val repo: UserSettingsRepo
) {
    suspend operator fun invoke(): Boolean = repo.syncToRemote()
}