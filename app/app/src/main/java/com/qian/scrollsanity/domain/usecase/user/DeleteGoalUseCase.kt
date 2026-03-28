package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.GoalRepo

class DeleteGoalUseCase(
    private val repo: GoalRepo
) {
    suspend operator fun invoke(userId: String, goalId: String) {
        repo.deleteGoal(userId, goalId)
    }
}