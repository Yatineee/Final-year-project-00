//package com.qian.scrollsanity.domain.usecase
//
//import android.util.Log
//import com.qian.scrollsanity.domain.model.intervention.InterventionContext
//import com.qian.scrollsanity.domain.model.intervention.InterventionDecision
//import com.qian.scrollsanity.domain.model.intervention.InterventionIntensity
//import com.qian.scrollsanity.domain.model.intervention.InterventionReason
//import com.qian.scrollsanity.domain.policy.app.FocusModePolicy
//import com.qian.scrollsanity.domain.policy.app.ScreenTimePolicy
//import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
//import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
//import com.qian.scrollsanity.domain.policy.app.TrackedAppsPolicy
//import com.qian.scrollsanity.domain.repo.PreferencesRepo
//import com.qian.scrollsanity.domain.repo.UsageRepo
//import com.qian.scrollsanity.domain.repo.LocalUsageRepo
//import kotlinx.coroutines.flow.first
//
//class EvaluateInterventionUseCase(
//    private val prefs: PreferencesRepo,
//    private val usage: UsageRepo,
//    private val localUsageRepo: LocalUsageRepo,
//    private val trackedAppsPolicy: TrackedAppsPolicy = TrackedAppsPolicy(),
//    private val screenTimePolicy: ScreenTimePolicy = ScreenTimePolicy(),
//    private val focusModePolicy: FocusModePolicy = FocusModePolicy()
//) {
//    companion object {
//        private const val TAG = "EvaluateInterventionUseCase"
//
//        // Session deviation parameters
//        private const val SESSION_BASELINE_DAYS = 7
//        private const val SESSION_Z_THRESHOLD = 2.0
//        private const val SESSION_SIGMA_MINUTES = 1.0
//        private const val MIN_CURRENT_SESSION_MINUTES = 1
//    }
//
//    /**
//     * currentSessionMinutes:
//     *   Length of the user's current ongoing session on the foreground tracked app.
//     */
//
//    suspend fun evaluate(
//        packageName: String,
//        currentSessionMinutes: Int
//    ): InterventionDecision {
//        val tracked = prefs.enabledTrackedApps.first()
//        Log.d(TAG, "Evaluating $packageName. Tracked packages: $tracked")
//
//        // 1) Only intervene on tracked apps
//        if (!trackedAppsPolicy.isTracked(packageName, tracked)) {
//            Log.d(TAG, "$packageName is not tracked. Allowing.")
//            return InterventionDecision.Allow
//        }
//
//        // 2) Focus mode check
//        val strict = prefs.focusStrictMode.first()
//        val focusActive = prefs.isFocusActive.first()
//
//        if (focusModePolicy.shouldBlockDuringFocus(strict, focusActive)) {
//            Log.d(TAG, "$packageName intervene by focus mode")
//            return InterventionDecision.Intervene(
//                packageName = packageName,
//                reason = InterventionReason.FocusSessionRestricted,
//                context = InterventionContext(
//                    usedMinutes = 0,
//                    limitMinutes = 0,
//                    intensity = InterventionIntensity.HIGH
//                )
//            )
//        }
//
//
//        // 3) Session deviation check
//        if (currentSessionMinutes >= MIN_CURRENT_SESSION_MINUTES) {
//            val sessions = localUsageRepo.getRecentSessions(
//                enabled = tracked,
//                days = SESSION_BASELINE_DAYS
//            )
//
//            val baseline = SessionBaselineCalculator.compute(sessions)
//            val sessionZ = SessionDeviationCalculator.computeZ(
//                currentSessionMinutes = currentSessionMinutes,
//                baseline = baseline,
//                sigmaMinutes = SESSION_SIGMA_MINUTES
//            )
//
//            Log.d(
//                TAG,
//                "Session deviation check: current=$currentSessionMinutes min, " +
//                        "baselineMedian=${baseline.medianMinutes}, " +
//                        "baselineMAD=${baseline.madMinutes}, " +
//                        "sampleCount=${baseline.sampleCount}, " +
//                        "sessionZ=$sessionZ"
//            )
//
//            if (baseline.sampleCount > 0 && sessionZ >= SESSION_Z_THRESHOLD) {
//                Log.d(TAG, "$packageName intervene - session deviation detected")
//                return InterventionDecision.Intervene(
//                    packageName = packageName,
//                    reason = InterventionReason.DailyLimitReached, // temporary reuse
//                    context = InterventionContext(
//                        usedMinutes = currentSessionMinutes,
//                        limitMinutes = baseline.medianMinutes.toInt(),
//                        intensity = InterventionIntensity.MEDIUM
//                    )
//                )
//            }
//        } else {
//            Log.d(
//                TAG,
//                "Skipping session deviation check: currentSessionMinutes=$currentSessionMinutes"
//            )
//        }
//
//        // 4) Daily limit check
//        val limit = prefs.dailyGoalMinutes.first()
//        val used = usage.getTotalUsageTodayMinutes(tracked)
//        Log.d(TAG, "Daily usage check: used=$used minutes, limit=$limit minutes")
//
//        if (screenTimePolicy.isOverLimit(used, limit)) {
//            Log.d(TAG, "$packageName intervene - daily limit reached")
//            return InterventionDecision.Intervene(
//                packageName = packageName,
//                reason = InterventionReason.DailyLimitReached,
//                context = InterventionContext(
//                    usedMinutes = used,
//                    limitMinutes = limit,
//                    intensity = InterventionIntensity.MEDIUM
//                )
//            )
//        }
//
//        Log.d(TAG, "$packageName allowed - under daily limit and no session deviation")
//        return InterventionDecision.Allow
//    }
//}
