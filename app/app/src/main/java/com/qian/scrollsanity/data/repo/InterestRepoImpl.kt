package com.qian.scrollsanity.data.repo

import com.qian.scrollsanity.data.remote.firestore.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.domain.repo.InterestRepo
import kotlinx.coroutines.flow.Flow

class InterestRepoImpl(
    private val firestoreRepo: FirestoreRepository
) : InterestRepo {

    override fun observeInterests(userId: String): Flow<List<InterestItem>> {
        return firestoreRepo.observeInterests(userId)
    }

    override suspend fun addInterest(userId: String, text: String) {
        firestoreRepo.addInterest(userId, text)
    }

    override suspend fun setInterestAchieved(userId: String, interestId: String, achieved: Boolean) {
        firestoreRepo.setInterestAchieved(userId, interestId, achieved)
    }

    override suspend fun deleteInterest(userId: String, interestId: String) {
        firestoreRepo.deleteInterest(userId, interestId)
    }
}