package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.GoalRepo

class AddGoalUseCase(
    private val repo: GoalRepo
) {
    suspend operator fun invoke(userId: String, text: String) {
        repo.addGoal(userId, text)
    }
}