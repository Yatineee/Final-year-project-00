package com.qian.scrollsanity.domain.policy

import com.qian.scrollsanity.data.TrackedAppId
import com.qian.scrollsanity.data.TrackedApps


class TrackedAppsPolicy {
    fun isTracked(pkg: String, enabledPackages: Set<String>): Boolean {
        return pkg in enabledPackages
    }
}
