package com.qian.scrollsanity.domain.usecase

import android.util.Log
import com.qian.scrollsanity.domain.model.BlockDecision
import com.qian.scrollsanity.domain.model.BlockReason
import com.qian.scrollsanity.domain.policy.FocusModePolicy
import com.qian.scrollsanity.domain.policy.ScreenTimePolicy
import com.qian.scrollsanity.domain.policy.TrackedAppsPolicy
import com.qian.scrollsanity.domain.repo.PreferencesRepo
import com.qian.scrollsanity.domain.repo.UsageRepo
import kotlinx.coroutines.flow.first

class ShouldBlockAppUseCase(
    private val prefs: PreferencesRepo,
    private val usage: UsageRepo,
    private val trackedAppsPolicy: TrackedAppsPolicy = TrackedAppsPolicy(),
    private val screenTimePolicy: ScreenTimePolicy = ScreenTimePolicy(),
    private val focusModePolicy: FocusModePolicy = FocusModePolicy(),
) {
    companion object {
        private const val TAG = "ShouldBlockAppUseCase"
    }

    suspend fun evaluate(packageName: String): BlockDecision {
        val tracked = prefs.enabledTrackedApps.first()
        Log.d(TAG, "Evaluating $packageName. Tracked packages: $tracked")

        // Only enforce for tracked apps
        if (!trackedAppsPolicy.isTracked(packageName, tracked)) {
            Log.d(TAG, "$packageName is not tracked. Allowing.")
            return BlockDecision.Allow
        }

        Log.d(TAG, "$packageName is tracked. Checking policies...")

        // Focus rule - blocks ALL tracked apps when focus mode is active
        val strict = prefs.focusStrictMode.first()
        val focusActive = prefs.isFocusActive.first()

        if (focusModePolicy.shouldBlockDuringFocus(strict, focusActive)) {
            Log.d(TAG, "$packageName blocked by focus mode (all tracked apps blocked)")
            return BlockDecision.Block(
                packageName = packageName,
                reason = BlockReason.FocusSessionRestricted,
                usedMinutes = 0,
                limitMinutes = 0
            )
        }

        // Daily goal rule
        val limit = prefs.dailyGoalMinutes.first()
        val used = usage.getTotalUsageTodayMinutes(tracked)
        Log.d(TAG, "Daily usage check: used=$used minutes, limit=$limit minutes")

        if (screenTimePolicy.isOverLimit(used, limit)) {
            Log.d(TAG, "$packageName blocked - daily limit reached")
            return BlockDecision.Block(
                packageName = packageName,
                reason = BlockReason.DailyLimitReached,
                usedMinutes = used,
                limitMinutes = limit
            )
        }

        Log.d(TAG, "$packageName allowed - under daily limit")
        return BlockDecision.Allow
    }
}
