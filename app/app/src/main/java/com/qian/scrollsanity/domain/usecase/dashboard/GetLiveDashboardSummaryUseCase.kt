package com.qian.scrollsanity.domain.usecase.dashboard

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.domain.model.dashboard.DailyAverageSessionPoint
import com.qian.scrollsanity.domain.model.dashboard.LiveDashboardSummary
import com.qian.scrollsanity.domain.model.usagedata.AppSession
import com.qian.scrollsanity.domain.policy.deviation.BehaviorSessionNormalizer
import com.qian.scrollsanity.domain.policy.deviation.SessionBaselineCalculator
import com.qian.scrollsanity.domain.policy.deviation.SessionDeviationCalculator
import com.qian.scrollsanity.domain.repo.DashboardMetricsRepo
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.usecase.intervention.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.intervention.IntensityProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.util.Log

class GetLiveDashboardSummaryUseCase(
    private val localUsageRepo: LocalUsageRepo,
    private val enabledTrackedProvider: EnabledTrackedProvider,
    private val dashboardMetricsRepo: DashboardMetricsRepo,
    private val intensityProvider: IntensityProvider
) {
    suspend operator fun invoke(
        currentSessionMinutes: Int?,
        inTrackedSession: Boolean
    ): LiveDashboardSummary {

        val enabledIds: Set<TrackedAppId> = enabledTrackedProvider.getEnabledTrackedIds()
        Log.d("LiveDashboard", "enabledIds=$enabledIds")
        Log.d("LiveDashboard", "inTrackedSession=$inTrackedSession")
        Log.d("LiveDashboard", "currentSessionMinutes=$currentSessionMinutes")

        val trackedUsageTodayMinutes = localUsageRepo.getTodayTotalMinutes(enabledIds)

        val enabledPackages: Set<String> =
            enabledIds.flatMap { id ->
                TrackedApps.metaFor(id).exactPackages
            }.toSet()

        val rawSessions: List<AppSession> =
            localUsageRepo.getRecentSessions(
                enabledPackages = enabledPackages,
                days = 8
            )
        Log.d("LiveDashboard", "enabledPackages=$enabledPackages")
        Log.d("LiveDashboard", "rawSessionsCount=${rawSessions.size}")
        val normalizedSessions: List<AppSession> =
            BehaviorSessionNormalizer.normalize(rawSessions)
        Log.d("LiveDashboard", "normalizedSessionsCount=${normalizedSessions.size}")
        Log.d(
            "LiveDashboard",
            "latestNormalizedSession=${normalizedSessions.maxByOrNull { it.endMillis }}"
        )
        val dailyAverageSessionPoints = buildDailyAverageSessionPoints(
            sessions = normalizedSessions,
            days = 8
        )

        val baseline = SessionBaselineCalculator.compute(normalizedSessions)

        Log.d(
            "InterventionCheck",
            "baseline sampleCount=${baseline.sampleCount}, median=${baseline.medianMinutes}, mad=${baseline.madMinutes}"
        )
        val latestNormalizedSession = normalizedSessions.maxByOrNull { it.endMillis }

        val effectiveSessionMinutes: Int? = when {
            inTrackedSession && currentSessionMinutes != null -> currentSessionMinutes
            else -> latestNormalizedSession?.durationMinutes
        }
        Log.d("LiveDashboard", "effectiveSessionMinutes=$effectiveSessionMinutes")

        val currentZScore: Double? =
            if (effectiveSessionMinutes != null && baseline.sampleCount > 0) {
                SessionDeviationCalculator.computeZ(
                    currentSessionMinutes = effectiveSessionMinutes,
                    baseline = baseline,
                    sigmaMinutes = 1.0
                )
            } else {
                null
            }
        Log.d("LiveDashboard", "currentZScore=$currentZScore")

        val intensity = intensityProvider.getIntensity()
        Log.d("LiveDashboard", "intensity=$intensity")

        val baseThreshold = when (intensity.lowercase()) {
            "low" -> 3.0
            "medium" -> 2.5
            "high" -> 2.3
            else -> 2.5
        }

        val storedThreshold = dashboardMetricsRepo.getLatestThreshold()
        val finalThreshold = storedThreshold ?: baseThreshold

        Log.d("LiveDashboard", "baseThreshold=$baseThreshold")
        Log.d("LiveDashboard", "storedThreshold=$storedThreshold")
        Log.d("LiveDashboard", "finalThreshold=$finalThreshold")

        return LiveDashboardSummary(
            currentZScore = currentZScore,
            interventionThreshold = finalThreshold,
            interventionsToday = dashboardMetricsRepo.getInterventionsToday(),
            trackedUsageTodayMinutes = trackedUsageTodayMinutes,
            inTrackedSession = inTrackedSession,
            currentSessionMinutes = effectiveSessionMinutes ?: 0,
            dailyAverageSessionPoints = dailyAverageSessionPoints
        )
    }

    private fun buildDailyAverageSessionPoints(
        sessions: List<AppSession>,
        days: Int = 8
    ): List<DailyAverageSessionPoint> {
        val calendar = Calendar.getInstance()

        val dayKeys = mutableListOf<String>()
        val dayLabels = mutableListOf<String>()

        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

        for (offset in (days - 1) downTo 0) {
            val cal = calendar.clone() as Calendar
            cal.add(Calendar.DAY_OF_YEAR, -offset)

            val date = cal.time
            dayKeys.add(keyFormat.format(date))
            dayLabels.add(labelFormat.format(date))
        }

        val sessionsByDayKey: Map<String, List<AppSession>> =
            sessions.groupBy { session ->
                keyFormat.format(Date(session.endMillis))
            }

        return dayKeys.mapIndexed { index, key ->
            val sessionsForDay = sessionsByDayKey[key].orEmpty()

            val avgMinutes =
                if (sessionsForDay.isNotEmpty()) {
                    sessionsForDay.map { it.durationMinutes }.average()
                } else {
                    0.0
                }

            DailyAverageSessionPoint(
                label = dayLabels[index],
                averageMinutes = avgMinutes
            )
        }
    }
}