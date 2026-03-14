package com.qian.scrollsanity.domain.policy.deviation

import kotlin.math.abs

data class RobustStats(val median: Double, val mad: Double)

class RobustZCalculator {

    private fun median(xs: List<Double>): Double {
        if (xs.isEmpty()) return 0.0
        val n = xs.size
        return if (n % 2 == 1) xs[n / 2]
        else (xs[n / 2 - 1] + xs[n / 2]) / 2.0
    }

    fun statsFromMinutes(minutes7d: List<Int>): RobustStats {
        val xs = minutes7d.map { it.toDouble() }.sorted()
        val med = median(xs)
        val absDevs = xs.map { abs(it - med) }.sorted()
        val mad = median(absDevs)
        return RobustStats(med, mad)
    }

    fun robustZ(todayMinutes: Int, stats: RobustStats, sigmaMin: Double = 1.0): Double {
        // sigmaMin 防止 MAD=0
        return (todayMinutes - stats.median) / (stats.mad + sigmaMin)
    }
}