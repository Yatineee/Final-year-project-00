package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.repo.GoalRepo

class SetGoalAchievedUseCase(
    private val repo: GoalRepo
) {
    suspend operator fun invoke(userId: String, goalId: String, achieved: Boolean) {
        repo.setGoalAchieved(userId, goalId, achieved)
    }
}