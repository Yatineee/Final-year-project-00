package com.qian.scrollsanity.domain.repo

interface DashboardMetricsRepo {
    suspend fun saveLatestZScore(z: Double)
    suspend fun getLatestZScore(): Double?

    suspend fun saveLatestThreshold(threshold: Double)
    suspend fun getLatestThreshold(): Double?

    suspend fun incrementInterventionsToday()
    suspend fun getInterventionsToday(): Int

    suspend fun resetInterventionsIfNewDay()

    suspend fun clearAll()
}