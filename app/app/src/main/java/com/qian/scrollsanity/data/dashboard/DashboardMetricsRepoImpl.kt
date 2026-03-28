package com.qian.scrollsanity.data.dashboard

import android.content.Context
import com.qian.scrollsanity.domain.repo.DashboardMetricsRepo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardMetricsRepoImpl(
    context: Context
) : DashboardMetricsRepo {

    private val prefs = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    override suspend fun saveLatestZScore(z: Double) {
        prefs.edit()
            .putLong(KEY_LATEST_Z_BITS, z.toBits())
            .putBoolean(KEY_HAS_LATEST_Z, true)
            .apply()
    }

    override suspend fun getLatestZScore(): Double? {
        val hasValue = prefs.getBoolean(KEY_HAS_LATEST_Z, false)
        if (!hasValue) return null

        val bits = prefs.getLong(KEY_LATEST_Z_BITS, 0L)
        return Double.fromBits(bits)
    }

    override suspend fun saveLatestThreshold(threshold: Double) {
        prefs.edit()
            .putLong(KEY_LATEST_THRESHOLD_BITS, threshold.toBits())
            .putBoolean(KEY_HAS_LATEST_THRESHOLD, true)
            .apply()
    }

    override suspend fun getLatestThreshold(): Double? {
        val hasValue = prefs.getBoolean(KEY_HAS_LATEST_THRESHOLD, false)
        if (!hasValue) return null

        val bits = prefs.getLong(KEY_LATEST_THRESHOLD_BITS, 0L)
        return Double.fromBits(bits)
    }

    override suspend fun incrementInterventionsToday() {
        resetInterventionsIfNewDay()

        val current = prefs.getInt(KEY_INTERVENTIONS_TODAY, 0)
        prefs.edit()
            .putInt(KEY_INTERVENTIONS_TODAY, current + 1)
            .putString(KEY_INTERVENTIONS_DATE, todayKey())
            .apply()
    }

    override suspend fun getInterventionsToday(): Int {
        resetInterventionsIfNewDay()
        return prefs.getInt(KEY_INTERVENTIONS_TODAY, 0)
    }

    override suspend fun resetInterventionsIfNewDay() {
        val storedDate = prefs.getString(KEY_INTERVENTIONS_DATE, null)
        val today = todayKey()

        if (storedDate != today) {
            prefs.edit()
                .putString(KEY_INTERVENTIONS_DATE, today)
                .putInt(KEY_INTERVENTIONS_TODAY, 0)
                .apply()
        }
    }

    override suspend fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun todayKey(): String {
        return SimpleDateFormat(DATE_PATTERN, Locale.US).format(Date())
    }

    companion object {
        private const val PREFS_NAME = "dashboard_metrics"
        private const val DATE_PATTERN = "yyyy-MM-dd"

        private const val KEY_LATEST_Z_BITS = "latest_z_bits"
        private const val KEY_HAS_LATEST_Z = "has_latest_z"

        private const val KEY_LATEST_THRESHOLD_BITS = "latest_threshold_bits"
        private const val KEY_HAS_LATEST_THRESHOLD = "has_latest_threshold"

        private const val KEY_INTERVENTIONS_TODAY = "interventions_today"
        private const val KEY_INTERVENTIONS_DATE = "interventions_date"
    }
}