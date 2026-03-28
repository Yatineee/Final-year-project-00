package com.qian.scrollsanity.domain.usecase.dashboard

import com.qian.scrollsanity.domain.repo.DashboardMetricsRepo

class RecordDashboardMetricsUseCase(
    private val dashboardMetricsRepo: DashboardMetricsRepo
) {
    suspend fun recordLatestEvaluation(
        z: Double,
        threshold: Double
    ) {
        dashboardMetricsRepo.saveLatestZScore(z)
        dashboardMetricsRepo.saveLatestThreshold(threshold)
    }

    suspend fun recordTriggeredIntervention() {
        dashboardMetricsRepo.incrementInterventionsToday()
    }


}

