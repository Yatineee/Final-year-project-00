package com.qian.scrollsanity.data.repo

import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.repo.GoalRepo
import kotlinx.coroutines.flow.Flow

class GoalRepoImpl(
    private val firestoreRepo: FirestoreRepository
) : GoalRepo {

    override fun observeGoals(userId: String): Flow<List<GoalItem>> {
        return firestoreRepo.observeGoals(userId)
    }

    override suspend fun addGoal(userId: String, text: String) {
        firestoreRepo.addGoal(userId, text)
    }

    override suspend fun setGoalAchieved(userId: String, goalId: String, achieved: Boolean) {
        firestoreRepo.setGoalAchieved(userId, goalId, achieved)
    }

    override suspend fun deleteGoal(userId: String, goalId: String) {
        firestoreRepo.deleteGoal(userId, goalId)
    }
}