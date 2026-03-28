package com.qian.scrollsanity.domain.policy.deviation

import com.qian.scrollsanity.domain.util.TrackedAppId
import com.qian.scrollsanity.domain.model.session.AppSession
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBaselineCalculatorTest {

    private fun session(durationMinutes: Int): AppSession {
        return AppSession(
            trackedAppId = TrackedAppId.YOUTUBE,
            packageName = "com.google.android.youtube",
            startMillis = 0L,
            endMillis = durationMinutes * 60_000L,
            durationMinutes = durationMinutes
        )
    }

    @Test
    fun compute_returnsZeroStats_whenSessionsAreEmpty() {
        val result = SessionBaselineCalculator.compute(emptyList())

        assertEquals(0.0, result.medianMinutes, 0.0001)
        assertEquals(0.0, result.madMinutes, 0.0001)
        assertEquals(0, result.sampleCount)
    }

    @Test
    fun compute_calculatesMedianAndMad_forOddNumberOfSessions() {
        val sessions = listOf(
            session(10),
            session(12),
            session(14)
        )

        val result = SessionBaselineCalculator.compute(sessions)

        assertEquals(12.0, result.medianMinutes, 0.0001)
        assertEquals(2.0, result.madMinutes, 0.0001)
        assertEquals(3, result.sampleCount)
    }

    @Test
    fun compute_calculatesMedianAndMad_forEvenNumberOfSessions() {
        val sessions = listOf(
            session(10),
            session(12),
            session(14),
            session(16)
        )

        val result = SessionBaselineCalculator.compute(sessions)

        assertEquals(13.0, result.medianMinutes, 0.0001)
        assertEquals(2.0, result.madMinutes, 0.0001)
        assertEquals(4, result.sampleCount)
    }

    @Test
    fun compute_sortsDurationsBeforeCalculatingStats() {
        val sessions = listOf(
            session(14),
            session(10),
            session(12)
        )

        val result = SessionBaselineCalculator.compute(sessions)

        assertEquals(12.0, result.medianMinutes, 0.0001)
        assertEquals(2.0, result.madMinutes, 0.0001)
        assertEquals(3, result.sampleCount)
    }

    @Test
    fun compute_returnsZeroMad_whenAllDurationsAreTheSame() {
        val sessions = listOf(
            session(10),
            session(10),
            session(10),
            session(10)
        )

        val result = SessionBaselineCalculator.compute(sessions)

        assertEquals(10.0, result.medianMinutes, 0.0001)
        assertEquals(0.0, result.madMinutes, 0.0001)
        assertEquals(4, result.sampleCount)
    }

    @Test
    fun compute_countsAllSamplesCorrectly() {
        val sessions = listOf(
            session(5),
            session(10),
            session(15),
            session(20),
            session(25)
        )

        val result = SessionBaselineCalculator.compute(sessions)

        assertEquals(5, result.sampleCount)
    }
}