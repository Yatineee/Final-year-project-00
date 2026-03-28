package com.qian.scrollsanity.domain.usecase.old.intervention

import android.util.Log
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.util.TrackedApps
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.deviation.BehaviorSessionNormalizer
import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.old.dashboard.RecordDashboardMetricsUseCase

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
        Log.d("InterventionCheck", "run started")
        Log.d("InterventionCheck", "nowMs=$nowMs")
        Log.d("InterventionCheck", "currentSessionMinutes=$currentSessionMinutes")
        Log.d("InterventionCheck", "state=$state")

        if (!state.inBlockedSession) {
            Log.d("InterventionCheck", "skip: inBlockedSession=false")
            return state to TriggerResult.None
        }

        val intensity = intensityProvider.getIntensity().trim().lowercase()
        val cooldown = cooldownPolicy.cooldownMs(intensity)
        val due = (nowMs - state.lastCheckAtMs) >= cooldown

        Log.d("InterventionCheck", "intensity=$intensity")
        Log.d("InterventionCheck", "cooldownMs=$cooldown")
        Log.d("InterventionCheck", "lastCheckAtMs=${state.lastCheckAtMs}")
        Log.d("InterventionCheck", "due=$due")

        if (!due) {
            Log.d("InterventionCheck", "skip: cooldown not due yet")
            return state to TriggerResult.None
        }

        val enabledIds: Set<TrackedAppId> = enabledProvider.getEnabledTrackedIds()
        val enabledPackages: Set<String> =
            enabledIds.flatMap { id ->
                TrackedApps.metaFor(id).exactPackages
            }.toSet()

        Log.d("InterventionCheck", "enabledIds=$enabledIds")
        Log.d("InterventionCheck", "enabledPackages=$enabledPackages")

        val rawSessions = localUsageRepo.getRecentSessions(
            enabledPackages = enabledPackages,
            days = 7
        ).filter { it.durationMinutes >= 1 }

        Log.d("InterventionCheck", "rawSessionsCount=${rawSessions.size}")

        if (rawSessions.isEmpty()) {
            Log.d("InterventionCheck", "skip: no recent sessions")
            val updatedState = state.copy(lastCheckAtMs = nowMs)
            return updatedState to TriggerResult.None
        }

        val normalizedSessions = BehaviorSessionNormalizer.normalize(rawSessions)

        Log.d("InterventionCheck", "normalizedSessionsCount=${normalizedSessions.size}")
        Log.d(
            "InterventionCheck",
            "latestNormalizedSession=${normalizedSessions.maxByOrNull { it.endMillis }}"
        )

        if (normalizedSessions.isEmpty()) {
            Log.d("InterventionCheck", "skip: normalizedSessions empty")
            val updatedState = state.copy(lastCheckAtMs = nowMs)
            return updatedState to TriggerResult.None
        }

        val baseline = SessionBaselineCalculator.compute(normalizedSessions)

        Log.d(
            "InterventionCheck",
            "baseline sampleCount=${baseline.sampleCount}, median=${baseline.medianMinutes}, mad=${baseline.madMinutes}"
        )

        if (baseline.sampleCount <= 0) {
            Log.d("InterventionCheck", "skip: baseline sampleCount <= 0")
            val updatedState = state.copy(lastCheckAtMs = nowMs)
            return updatedState to TriggerResult.None
        }

        val z = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = currentSessionMinutes,
            baseline = baseline,
            sigmaMinutes = 1.0
        )

        Log.d("InterventionCheck", "z=$z")

        val threshold = deviationPolicy.interventionThreshold(intensity)
        Log.d("InterventionCheck", "threshold=$threshold")

        // Record latest evaluated values for dashboard display
        recordDashboardMetricsUseCase.recordNextInterventionThreshold(threshold)

        val eval = deviationPolicy.evaluate(
            z = z,
            intensity = intensity,
            alreadyAskedThisSession = state.askedUsageTypeThisSession
        )

        Log.d(
            "InterventionCheck",
            "eval ask=${eval.shouldAskUsageCheck}, trigger=${eval.shouldTriggerIntervention}, zScore=${eval.zScore}"
        )

        val shouldResetAskState = deviationPolicy.shouldResetAskState(z)
        Log.d("InterventionCheck", "shouldResetAskState=$shouldResetAskState")

        val updatedState = when {
            eval.shouldAskUsageCheck -> {
                val newState = state.copy(
                    lastCheckAtMs = nowMs,
                    askedUsageTypeThisSession = true
                )
                Log.d("InterventionCheck", "state updated for AskUsageType: $newState")
                newState
            }

            shouldResetAskState -> {
                val newState = state.copy(
                    lastCheckAtMs = nowMs,
                    askedUsageTypeThisSession = false
                )
                Log.d("InterventionCheck", "state reset ask flag: $newState")
                newState
            }

            else -> {
                val newState = state.copy(lastCheckAtMs = nowMs)
                Log.d("InterventionCheck", "state updated with lastCheckAtMs only: $newState")
                newState
            }
        }

        return when {
            eval.shouldAskUsageCheck -> {
                Log.d("InterventionCheck", "return AskUsageType")
                updatedState to TriggerResult.AskUsageType
            }

            eval.shouldTriggerIntervention -> {
                Log.d("InterventionCheck", "return TriggerIntervention z=$z")
                recordDashboardMetricsUseCase.recordTriggeredIntervention()
                updatedState to TriggerResult.TriggerIntervention(z)
            }

            else -> {
                Log.d("InterventionCheck", "return None")
                updatedState to TriggerResult.None
            }
        }
    }
}