package com.qian.scrollsanity.blocker.controller

import android.content.Context
import android.util.Log
import com.qian.scrollsanity.data.local.usagestats.UsageStatsRepository
import com.qian.scrollsanity.data.repo.DashboardMetricsRepoImpl
import com.qian.scrollsanity.data.old.perferences.PreferencesManager
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.decision.NextInterventionThresholdPolicy
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.old.dashboard.RecordDashboardMetricsUseCase
import com.qian.scrollsanity.domain.usecase.old.intervention.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.old.intervention.IntensityProvider
import com.qian.scrollsanity.domain.usecase.old.intervention.MaybeRunInterventionCheckUseCase
import com.qian.scrollsanity.domain.usecase.old.intervention.TriggerResult
import kotlinx.coroutines.flow.first

class BlockerRuntimeFacade(
    private val context: Context
) {
    companion object {
        private const val TAG = "BlockerRuntimeFacade"
    }

    private val cooldownPolicy = CooldownPolicy()
    private val deviationPolicy = DeviationInterventionPolicy()
    private val nextThresholdPolicy = NextInterventionThresholdPolicy()

    private val prefsManager by lazy { PreferencesManager(context) }
    private val usageRepoReal by lazy { UsageStatsRepository(context) }
    private val localUsageRepo: LocalUsageRepo by lazy { usageRepoReal }

    private val dashboardMetricsRepo by lazy { DashboardMetricsRepoImpl(context) }
    private val recordDashboardMetricsUseCase by lazy {
        RecordDashboardMetricsUseCase(dashboardMetricsRepo)
    }

    suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
        return prefsManager.enabledTrackedApps.first()
    }

    suspend fun getIntensity(): String {
        val value = prefsManager.interventionIntensity.first()
        Log.d(TAG, "getIntensity -> $value")
        return value
    }

    suspend fun runInterventionCheck(
        nowMs: Long,
        state: InterventionSessionState,
        currentSessionMinutes: Int
    ): Pair<InterventionSessionState, TriggerResult> {
        val intensityProvider = object : IntensityProvider {
            override suspend fun getIntensity(): String {
                return prefsManager.interventionIntensity.first()
            }
        }

        val enabledProvider = object : EnabledTrackedProvider {
            override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
                return prefsManager.enabledTrackedApps.first()
            }
        }

        val useCase = MaybeRunInterventionCheckUseCase(
            cooldownPolicy = cooldownPolicy,
            deviationPolicy = deviationPolicy,
            intensityProvider = intensityProvider,
            enabledProvider = enabledProvider,
            localUsageRepo = localUsageRepo,
            recordDashboardMetricsUseCase = recordDashboardMetricsUseCase
        )

        return useCase.run(
            nowMs = nowMs,
            state = state,
            currentSessionMinutes = currentSessionMinutes
        )
    }

    suspend fun recordNextThresholdAfterTrigger(triggeredZ: Double) {
        val intensity = prefsManager.interventionIntensity.first()

        val nextThreshold = nextThresholdPolicy.computeNextThreshold(
            intensity = intensity,
            triggeredZ = triggeredZ
        )

        Log.d(
            TAG,
            "recordNextThresholdAfterTrigger intensity=$intensity triggeredZ=$triggeredZ nextThreshold=$nextThreshold"
        )

        recordDashboardMetricsUseCase.recordNextInterventionThreshold(nextThreshold)
    }
}