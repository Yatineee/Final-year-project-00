package com.qian.scrollsanity.domain.policy.deviation

import com.qian.scrollsanity.domain.model.deviation.SessionBaselineStats

/**
 * Computes a robust deviation score for the current session duration
 * against the recent historical session baseline.
 *
 * Baseline is defined by:
 * - median session duration
 * - MAD (median absolute deviation)
 *
 * Formula:
 *   z = (currentSessionMinutes - medianMinutes) / (madMinutes + sigmaMinutes)
 *
 * This is a robust Z-like score, not a mean/std-based standard z-score.
 */


object SessionDeviationCalculator {

    fun computeZ(
        currentSessionMinutes: Int,
        baseline: SessionBaselineStats,
        sigmaMinutes: Double = 1.0
    ): Double {
        if (baseline.sampleCount <= 0) return 0.0

        return (currentSessionMinutes - baseline.medianMinutes) /
                (baseline.madMinutes + sigmaMinutes)
    }
}