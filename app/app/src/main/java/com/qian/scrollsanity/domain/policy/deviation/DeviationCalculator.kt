//package com.qian.scrollsanity.domain.policy.deviation
//
//import kotlin.math.abs
//
//object DeviationCalculator {
//    fun computeZ(todayMinutes: Int, baselineDaysMinutes: List<Int>, sigmaMin: Double = 1.0): Double {
//        if (baselineDaysMinutes.isEmpty()) return 0.0
//
//        val xs = baselineDaysMinutes.map { it.toDouble() }.sorted()
//        val med = median(xs)
//        val absDevs = xs.map { abs(it - med) }.sorted()
//        val mad = median(absDevs)
//
//        return (todayMinutes - med) / (mad + sigmaMin)
//    }
//
//    private fun median(xs: List<Double>): Double {
//        if (xs.isEmpty()) return 0.0
//        val n = xs.size
//        return if (n % 2 == 1) xs[n / 2]
//        else (xs[n / 2 - 1] + xs[n / 2]) / 2.0
//    }
//}