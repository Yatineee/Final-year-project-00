package com.qian.scrollsanity.domain.usecase.dashboard

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.domain.model.dashboard.DashboardSummary
import com.qian.scrollsanity.domain.repo.DashboardMetricsRepo
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.usecase.intervention.EnabledTrackedProvider

class GetDashboardSummaryUseCase(
    private val localUsageRepo: LocalUsageRepo,
    private val enabledTrackedProvider: EnabledTrackedProvider,
    private val dashboardMetricsRepo: DashboardMetricsRepo
) {
    suspend operator fun invoke(): DashboardSummary {
        val enabledIds: Set<TrackedAppId> = enabledTrackedProvider.getEnabledTrackedIds()

        val trackedUsageTodayMinutes = localUsageRepo.getTodayTotalMinutes(enabledIds)

        return DashboardSummary(
            currentZScore = dashboardMetricsRepo.getLatestZScore(),
            interventionThreshold = dashboardMetricsRepo.getLatestThreshold(),
            interventionsToday = dashboardMetricsRepo.getInterventionsToday(),
            trackedUsageTodayMinutes = trackedUsageTodayMinutes
        )
    }
}