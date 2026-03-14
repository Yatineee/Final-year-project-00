package com.qian.scrollsanity.domain.policy.deviation

import com.qian.scrollsanity.domain.model.deviation.SessionBaselineStats

/**
 * Computes robust deviation score (Z-like score) for the current session length
 * against historical session baseline statistics.
 *
 * Formula:
 *   z = (currentSessionMinutes - medianMinutes) / (madMinutes + sigmaMinutes)
 *
 * sigmaMinutes is a smoothing term to avoid division by zero
 * when MAD is very small or zero.
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