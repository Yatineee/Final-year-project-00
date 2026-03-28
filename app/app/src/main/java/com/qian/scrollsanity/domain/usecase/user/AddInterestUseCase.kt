package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.InterestRepo

class AddInterestUseCase(
    private val repo: InterestRepo
) {
    suspend operator fun invoke(userId: String, text: String) {
        repo.addInterest(userId, text)
    }
}