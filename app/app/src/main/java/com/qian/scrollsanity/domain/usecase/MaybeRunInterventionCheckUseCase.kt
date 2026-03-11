package com.qian.scrollsanity.domain.usecase

import com.qian.scrollsanity.data.TrackedAppId
import com.qian.scrollsanity.domain.policy.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.DeviationCalculator
import com.qian.scrollsanity.domain.trigger.CooldownPolicy
import com.qian.scrollsanity.domain.trigger.InterventionSessionState

sealed class TriggerResult {
    object None : TriggerResult()
    object AskUsageType : TriggerResult()                 // z >= 2
    data class TriggerIntervention(val z: Double) : TriggerResult() // 达到强度阈值
}

/** 只负责提供强度：LOW|MEDIUM|HIGH */
interface IntensityProvider {
    suspend fun getIntensity(): String
}

/** 提供当前用户启用的 tracked apps ids */
interface EnabledTrackedProvider {
    suspend fun getEnabledTrackedIds(): Set<TrackedAppId>
}

/** 本地 usage 提供者：今日 + baseline 7天（建议不含今天） */
interface LocalUsageProvider {
    suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int
    suspend fun getBaseline7DaysMinutes(enabled: Set<TrackedAppId>): List<Int>
}

class MaybeRunInterventionCheckUseCase(
    private val cooldownPolicy: CooldownPolicy,
    private val deviationPolicy: DeviationInterventionPolicy,
    private val intensityProvider: IntensityProvider,
    private val enabledProvider: EnabledTrackedProvider,
    private val usageProvider: LocalUsageProvider
) {
    suspend fun run(
        nowMs: Long,
        state: InterventionSessionState
    ): Pair<InterventionSessionState, TriggerResult> {

        if (!state.inBlockedSession) return state to TriggerResult.None

        val intensity = intensityProvider.getIntensity().trim().uppercase()
        val cooldown = cooldownPolicy.cooldownMs(intensity)

        val due = (nowMs - state.lastCheckAtMs) >= cooldown
        if (!due) return state to TriggerResult.None

        val enabled = enabledProvider.getEnabledTrackedIds()

        // 1) 今日 minutes + baseline 7天
        val todayMinutes = usageProvider.getTodayTotalMinutes(enabled)
        val baseline7 = usageProvider.getBaseline7DaysMinutes(enabled)

        // 2) z
        val z = DeviationCalculator.computeZ(
            todayMinutes = todayMinutes,
            baselineDaysMinutes = baseline7,
            sigmaMin = 1.0
        )

        // 3) policy
        deviationPolicy.resetIfRecovered(z)
        val eval = deviationPolicy.evaluate(z, intensity.lowercase())

        val newState = state.copy(lastCheckAtMs = nowMs)

        return when {
            eval.shouldAskUsageCheck && !state.askedUsageTypeThisSession ->
                newState.copy(askedUsageTypeThisSession = true) to TriggerResult.AskUsageType

            eval.shouldTriggerIntervention ->
                newState to TriggerResult.TriggerIntervention(z)

            else -> newState to TriggerResult.None
        }
    }
}