package com.qian.scrollsanity.domain.model.deviation

/**
 * Baseline statistics computed from historical app sessions.
 *
 * medianMinutes:
 *   The median session length in minutes.
 *
 * madMinutes:
 *   Median Absolute Deviation (MAD) of session lengths in minutes.
 *
 * sampleCount:
 *   Number of sessions used to compute the baseline.
 */
data class SessionBaselineStats(
    val medianMinutes: Double,
    val madMinutes: Double,
    val sampleCount: Int
)