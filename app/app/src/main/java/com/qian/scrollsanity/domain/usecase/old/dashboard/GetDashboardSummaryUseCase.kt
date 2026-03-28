package com.qian.scrollsanity.domain.usecase.old.dashboard

import com.qian.scrollsanity.domain.model.old.dashboard.DashboardSummary
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.repo.old.DashboardMetricsRepo
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.usecase.old.intervention.EnabledTrackedProvider
/**
 * Legacy dashboard summary use case.
 *
 * This use case reads persisted dashboard metrics from repo.
 * It is kept temporarily during migration to live dashboard summary.
 */



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