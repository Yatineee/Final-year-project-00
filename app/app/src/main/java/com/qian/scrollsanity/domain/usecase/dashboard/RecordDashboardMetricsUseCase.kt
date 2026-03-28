package com.qian.scrollsanity.domain.usecase.dashboard

import com.qian.scrollsanity.domain.repo.DashboardMetricsRepo

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