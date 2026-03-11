package com.qian.scrollsanity.domain.repo

import com.qian.scrollsanity.data.TrackedAppId

interface InterventionConfigRepo {
    suspend fun getIntensity(): String                 // low/medium/high
    suspend fun getEnabledTrackedIds(): Set<TrackedAppId>
}