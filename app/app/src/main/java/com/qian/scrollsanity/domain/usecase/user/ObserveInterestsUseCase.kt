package com.qian.scrollsanity.domain.usecase.user

import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.domain.repo.InterestRepo
import kotlinx.coroutines.flow.Flow

class ObserveInterestsUseCase(
    private val repo: InterestRepo
) {
    operator fun invoke(userId: String): Flow<List<InterestItem>> = repo.observeInterests(userId)
}