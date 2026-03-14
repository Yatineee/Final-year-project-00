package com.qian.scrollsanity.data.config

import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.domain.repo.InterventionConfigRepo
import kotlinx.coroutines.flow.first

class InterventionConfigRepoImpl(
    private val prefs: PreferencesManager
) : InterventionConfigRepo {

    override suspend fun getIntensity(): String {
        // 需要你在 PreferencesManager 做 interventionIntensity flow
        return prefs.interventionIntensity.first().lowercase()
    }

    override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
        return prefs.enabledTrackedApps.first()
    }
}