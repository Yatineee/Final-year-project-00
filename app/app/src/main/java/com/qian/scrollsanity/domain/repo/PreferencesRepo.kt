package com.qian.scrollsanity.domain.repo

import kotlinx.coroutines.flow.Flow

interface PreferencesRepo {
    val dailyGoalMinutes: Flow<Int>
    val enabledTrackedApps: Flow<Set<String>>

    // You already have this in PreferencesManager
    val focusStrictMode: Flow<Boolean>

    // If you don’t have sessions yet, your adapter can just return flowOf(false/emptySet)
    val isFocusActive: Flow<Boolean>
    val focusBlockedApps: Flow<Set<String>>
}
