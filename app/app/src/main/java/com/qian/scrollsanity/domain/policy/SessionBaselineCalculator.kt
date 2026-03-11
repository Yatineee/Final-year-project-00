package com.qian.scrollsanity.domain.policy

import com.qian.scrollsanity.domain.model.AppSession
import com.qian.scrollsanity.domain.model.SessionBaselineStats
import kotlin.math.abs

object SessionBaselineCalculator {

    /**
     * Computes robust baseline statistics from historical sessions.
     *
     * Steps:
     * 1. Extract durationMinutes from sessions
     * 2. Sort durations
     * 3. Compute median
     * 4. Compute MAD = median(|x - median|)
     */
    fun compute(sessions: List<AppSession>): SessionBaselineStats {
        val durations = sessions
            .map { it.durationMinutes }
            .sorted()

        if (durations.isEmpty()) {
            return SessionBaselineStats(
                medianMinutes = 0.0,
                madMinutes = 0.0,
                sampleCount = 0
            )
        }

        val median = medianOfInts(durations)

        val absDevs = durations
            .map { abs(it - median) }
            .sorted()

        val mad = medianOfDoubles(absDevs)

        return SessionBaselineStats(
            medianMinutes = median,
            madMinutes = mad,
            sampleCount = durations.size
        )
    }

    private fun medianOfInts(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0

        val n = values.size
        return if (n % 2 == 1) {
            values[n / 2].toDouble()
        } else {
            (values[n / 2 - 1] + values[n / 2]) / 2.0
        }
    }

    private fun medianOfDoubles(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0

        val n = values.size
        return if (n % 2 == 1) {
            values[n / 2]
        } else {
            (values[n / 2 - 1] + values[n / 2]) / 2.0
        }
    }
}