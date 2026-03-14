package com.qian.scrollsanity.domain.policy.app


class TrackedAppsPolicy {
    fun isTracked(pkg: String, enabledPackages: Set<String>): Boolean {
        return pkg in enabledPackages
    }
}
