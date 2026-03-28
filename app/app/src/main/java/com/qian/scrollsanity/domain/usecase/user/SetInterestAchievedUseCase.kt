package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.InterestRepo

class SetInterestAchievedUseCase(
    private val repo: InterestRepo
) {
    suspend operator fun invoke(userId: String, interestId: String, achieved: Boolean) {
        repo.setInterestAchieved(userId, interestId, achieved)
    }
}