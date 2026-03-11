package com.qian.scrollsanity.domain.usecase

import com.qian.scrollsanity.domain.policy.TrackedAppsPolicy
import com.qian.scrollsanity.domain.trigger.InterventionSessionState

class OnForegroundAppChangedUseCase(
    private val trackedAppsPolicy: TrackedAppsPolicy
) {
    /**
     * enabledPackages should be: enabled tracked app packages (exact package names)
     */
    fun onAppChanged(
        pkg: String,
        enabledPackages: Set<String>,
        state: InterventionSessionState
    ): InterventionSessionState {

        val isTracked = trackedAppsPolicy.isTracked(pkg, enabledPackages)

        return if (isTracked) {
            state.copy(
                inBlockedSession = true,
                currentPackage = pkg
            )
        } else {
            state.copy(
                inBlockedSession = false,
                currentPackage = null,
                askedUsageTypeThisSession = false
            )
        }
    }
}