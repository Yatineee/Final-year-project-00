package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.domain.model.user.InterestItem
import kotlinx.coroutines.flow.Flow

interface InterestRepo {
    fun observeInterests(userId: String): Flow<List<InterestItem>>
    suspend fun addInterest(userId: String, text: String)
    suspend fun setInterestAchieved(userId: String, interestId: String, achieved: Boolean)
    suspend fun deleteInterest(userId: String, interestId: String)
}