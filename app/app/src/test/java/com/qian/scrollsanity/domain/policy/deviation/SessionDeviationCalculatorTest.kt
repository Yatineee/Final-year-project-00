package com.qian.scrollsanity.domain.policy.deviation

import com.qian.scrollsanity.domain.model.deviation.SessionBaselineStats
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDeviationCalculatorTest {

    @Test
    fun computeZ_returnsZero_whenBaselineHasNoSamples() {
        val baseline = SessionBaselineStats(
            medianMinutes = 0.0,
            madMinutes = 0.0,
            sampleCount = 0
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 20,
            baseline = baseline
        )

        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun computeZ_returnsZero_whenCurrentSessionEqualsMedian() {
        val baseline = SessionBaselineStats(
            medianMinutes = 12.0,
            madMinutes = 2.0,
            sampleCount = 3
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 12,
            baseline = baseline
        )

        assertEquals(0.0, result, 0.0001)
    }

    @Test
    fun computeZ_returnsPositiveValue_whenCurrentSessionIsAboveMedian() {
        val baseline = SessionBaselineStats(
            medianMinutes = 12.0,
            madMinutes = 2.0,
            sampleCount = 3
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 20,
            baseline = baseline
        )

        assertEquals(8.0 / 3.0, result, 0.0001)
    }

    @Test
    fun computeZ_returnsNegativeValue_whenCurrentSessionIsBelowMedian() {
        val baseline = SessionBaselineStats(
            medianMinutes = 12.0,
            madMinutes = 2.0,
            sampleCount = 3
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 6,
            baseline = baseline
        )

        assertEquals(-6.0 / 3.0, result, 0.0001)
    }

    @Test
    fun computeZ_usesSigmaMinutesToAvoidDivisionByZero_whenMadIsZero() {
        val baseline = SessionBaselineStats(
            medianMinutes = 10.0,
            madMinutes = 0.0,
            sampleCount = 4
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 15,
            baseline = baseline
        )

        assertEquals(5.0, result, 0.0001)
    }

    @Test
    fun computeZ_usesCustomSigmaMinutes_whenProvided() {
        val baseline = SessionBaselineStats(
            medianMinutes = 10.0,
            madMinutes = 0.0,
            sampleCount = 4
        )

        val result = SessionDeviationCalculator.computeZ(
            currentSessionMinutes = 15,
            baseline = baseline,
            sigmaMinutes = 2.0
        )

        assertEquals(2.5, result, 0.0001)
    }
}