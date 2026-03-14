package com.qian.scrollsanity.domain.usecase.intervention

import android.util.Log
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
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

        val intensity = intensityProvider.getIntensity().trim().uppercase()
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

        val newState = state.copy(lastCheckAtMs = nowMs)

        if (sessions.isEmpty()) {
            Log.d("InterventionCheck", "No recent sessions found, skip trigger")
            return newState to TriggerResult.None
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

        deviationPolicy.resetIfRecovered(z)
        val eval = deviationPolicy.evaluate(z, intensity.lowercase())

        Log.d(
            "InterventionCheck",
            "eval: shouldAskUsageCheck=${eval.shouldAskUsageCheck}, shouldTriggerIntervention=${eval.shouldTriggerIntervention}, askedUsageTypeThisSession=${state.askedUsageTypeThisSession}"
        )

        return when {
            eval.shouldAskUsageCheck && !state.askedUsageTypeThisSession -> {
                Log.d("InterventionCheck", "Result: AskUsageType")
                newState.copy(askedUsageTypeThisSession = true) to TriggerResult.AskUsageType
            }

            eval.shouldTriggerIntervention -> {
                Log.d("InterventionCheck", "Result: TriggerIntervention(z=$z)")
                newState to TriggerResult.TriggerIntervention(z)
            }

            else -> {
                Log.d("InterventionCheck", "Result: None")
                newState to TriggerResult.None
            }
        }
    }
}