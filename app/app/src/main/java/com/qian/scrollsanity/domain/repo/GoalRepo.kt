package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.domain.model.user.GoalItem
import kotlinx.coroutines.flow.Flow

interface GoalRepo {
    fun observeGoals(userId: String): Flow<List<GoalItem>>
    suspend fun addGoal(userId: String, text: String)
    suspend fun setGoalAchieved(userId: String, goalId: String, achieved: Boolean)
    suspend fun deleteGoal(userId: String, goalId: String)
}