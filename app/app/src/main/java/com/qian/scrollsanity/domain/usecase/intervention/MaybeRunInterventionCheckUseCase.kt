package com.qian.scrollsanity.domain.usecase.intervention

import android.util.Log
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState

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
 *
 * Responsibilities:
 * - verify session gating conditions
 * - verify cooldown
 * - load recent tracked sessions
 * - compute deviation z-score
 * - evaluate pure decision rules through policy
 * - update session memory state
 * - return the next action for upper layers
 */
class MaybeRunInterventionCheckUseCase(
    private val cooldownPolicy: CooldownPolicy,
    private val deviationPolicy: DeviationInterventionPolicy,
    private val intensityProvider: IntensityProvider,
    private val enabledProvider: EnabledTrackedProvider,
    private val localUsageRepo: LocalUsageRepo
) {
    suspend fun run(
        nowMs: Long,
        state: InterventionSessionState,
        currentSessionMinutes: Int
    ): Pair<InterventionSessionState, TriggerResult> {

        if (!state.inBlockedSession) {
            Log.d("InterventionCheck", "Skip: not in blocked session")
            return state to TriggerResult.None
        }

        val intensity = intensityProvider.getIntensity().trim().lowercase()
        val cooldown = cooldownPolicy.cooldownMs(intensity)

        val due = (nowMs - state.lastCheckAtMs) >= cooldown
        if (!due) {
            Log.d(
                "InterventionCheck",
                "Skip: cooldown not due, nowMs=$nowMs, lastCheckAtMs=${state.lastCheckAtMs}, cooldown=$cooldown"
            )
            return state to TriggerResult.None
        }

        val enabledIds = enabledProvider.getEnabledTrackedIds()
        val enabledPackages: Set<String> =
            enabledIds.flatMap { id -> TrackedApps.metaFor(id).exactPackages }.toSet()

        val sessions = localUsageRepo.getRecentSessions(
            enabledPackages = enabledPackages,
            days = 7
        ).filter { it.durationMinutes >= 1 }

        Log.d(
            "InterventionCheck",
            "enabledPackages=$enabledPackages, sessions=${sessions.size}, currentSessionMinutes=$currentSessionMinutes"
        )

        if (sessions.isEmpty()) {
            val updatedState = state.copy(lastCheckAtMs = nowMs)
            Log.d(
                "InterventionCheck",
                "No recent sessions found after filtering (duration >= 1 min), skip trigger"
            )
            return updatedState to TriggerResult.None
        }

        val baseline = SessionBaselineCalculator.compute(sessions)

        val z = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = currentSessionMinutes,
            baseline = baseline,
            sigmaMinutes = 1.0
        )

        Log.d(
            "InterventionCheck",
            "currentSessionMinutes=$currentSessionMinutes, sessions=${sessions.size}, median=${baseline.medianMinutes}, mad=${baseline.madMinutes}, sampleCount=${baseline.sampleCount}, z=$z, intensity=$intensity"
        )

        val eval = deviationPolicy.evaluate(
            z = z,
            intensity = intensity,
            alreadyAskedThisSession = state.askedUsageTypeThisSession
        )

        val shouldResetAskState = deviationPolicy.shouldResetAskState(z)

        Log.d(
            "InterventionCheck",
            "eval: shouldAskUsageCheck=${eval.shouldAskUsageCheck}, shouldTriggerIntervention=${eval.shouldTriggerIntervention}, askedUsageTypeThisSession=${state.askedUsageTypeThisSession}, shouldResetAskState=$shouldResetAskState"
        )

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
                Log.d("InterventionCheck", "Result: AskUsageType")
                updatedState to TriggerResult.AskUsageType
            }

            eval.shouldTriggerIntervention -> {
                Log.d("InterventionCheck", "Result: TriggerIntervention(z=$z)")
                updatedState to TriggerResult.TriggerIntervention(z)
            }

            else -> {
                Log.d("InterventionCheck", "Result: None")
                updatedState to TriggerResult.None
            }
        }
    }
}