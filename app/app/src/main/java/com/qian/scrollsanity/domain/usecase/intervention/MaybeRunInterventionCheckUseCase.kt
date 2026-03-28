package com.qian.scrollsanity.domain.usecase.intervention

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.dashboard.RecordDashboardMetricsUseCase

sealed class TriggerResult {
    object None : TriggerResult()
    object AskUsageType : TriggerResult()
    data class TriggerIntervention(val z: Double) : TriggerResult()
}

interface IntensityProvider {
    suspend fun getIntensity(): String
}

interface EnabledTrackedProvider {
    suspend fun getEnabledTrackedIds(): Set<TrackedAppId>
}

/**
 * Checks whether the current tracked-session behavior deviates enough from
 * recent baseline behavior to require a usage check or intervention.
 */
class MaybeRunInterventionCheckUseCase(
    private val cooldownPolicy: CooldownPolicy,
    private val deviationPolicy: DeviationInterventionPolicy,
    private val intensityProvider: IntensityProvider,
    private val enabledProvider: EnabledTrackedProvider,
    private val localUsageRepo: LocalUsageRepo,
    private val recordDashboardMetricsUseCase: RecordDashboardMetricsUseCase
) {
    suspend fun run(
        nowMs: Long,
        state: InterventionSessionState,
        currentSessionMinutes: Int
    ): Pair<InterventionSessionState, TriggerResult> {

        if (!state.inBlockedSession) {
            return state to TriggerResult.None
        }

        val intensity = intensityProvider.getIntensity().trim().lowercase()
        val cooldown = cooldownPolicy.cooldownMs(intensity)

        val due = (nowMs - state.lastCheckAtMs) >= cooldown
        if (!due) {
            return state to TriggerResult.None
        }

        val enabledIds = enabledProvider.getEnabledTrackedIds()
        val enabledPackages: Set<String> =
            enabledIds.flatMap { id -> TrackedApps.metaFor(id).exactPackages }.toSet()

        val sessions = localUsageRepo.getRecentSessions(
            enabledPackages = enabledPackages,
            days = 7
        ).filter { it.durationMinutes >= 1 }

        if (sessions.isEmpty()) {
            val updatedState = state.copy(lastCheckAtMs = nowMs)
            return updatedState to TriggerResult.None
        }

        val baseline = SessionBaselineCalculator.compute(sessions)

        val z = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = currentSessionMinutes,
            baseline = baseline,
            sigmaMinutes = 1.0
        )

        val threshold = deviationPolicy.interventionThreshold(intensity)

        // Record latest evaluated values for dashboard display
        recordDashboardMetricsUseCase.recordLatestEvaluation(
            z = z,
            threshold = threshold
        )

        val eval = deviationPolicy.evaluate(
            z = z,
            intensity = intensity,
            alreadyAskedThisSession = state.askedUsageTypeThisSession
        )

        val shouldResetAskState = deviationPolicy.shouldResetAskState(z)

        val updatedState = when {
            eval.shouldAskUsageCheck -> {
                state.copy(
                    lastCheckAtMs = nowMs,
                    askedUsageTypeThisSession = true
                )
            }

            shouldResetAskState -> {
                state.copy(
                    lastCheckAtMs = nowMs,
                    askedUsageTypeThisSession = false
                )
            }

            else -> {
                state.copy(lastCheckAtMs = nowMs)
            }
        }

        return when {
            eval.shouldAskUsageCheck -> {
                updatedState to TriggerResult.AskUsageType
            }

            eval.shouldTriggerIntervention -> {
                recordDashboardMetricsUseCase.recordTriggeredIntervention()
                updatedState to TriggerResult.TriggerIntervention(z)
            }

            else -> {
                updatedState to TriggerResult.None
            }
        }
    }


}