package com.qian.scrollsanity.domain.usecase.old.dashboard

import com.qian.scrollsanity.domain.repo.old.DashboardMetricsRepo

class RecordDashboardMetricsUseCase(
    private val dashboardMetricsRepo: DashboardMetricsRepo
) {
    suspend fun recordTriggeredIntervention() {
        dashboardMetricsRepo.incrementInterventionsToday()
    }

    suspend fun recordNextInterventionThreshold(threshold: Double) {
        dashboardMetricsRepo.saveLatestThreshold(threshold)
    }
}