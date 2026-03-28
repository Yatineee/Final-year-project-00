package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.InterestRepo

class DeleteInterestUseCase(
    private val repo: InterestRepo
) {
    suspend operator fun invoke(userId: String, interestId: String) {
        repo.deleteInterest(userId, interestId)
    }
}