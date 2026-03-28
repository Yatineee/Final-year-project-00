package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.repo.GoalRepo
import kotlinx.coroutines.flow.Flow

class ObserveGoalsUseCase(
    private val repo: GoalRepo
) {
    operator fun invoke(userId: String): Flow<List<GoalItem>> = repo.observeGoals(userId)
}