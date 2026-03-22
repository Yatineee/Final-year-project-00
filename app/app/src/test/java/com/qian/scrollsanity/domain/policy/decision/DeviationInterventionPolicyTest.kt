package com.qian.scrollsanity.domain.policy.decision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeviationInterventionPolicyTest {

    private lateinit var policy: DeviationInterventionPolicy

    @Before
    fun setUp() {
        policy = DeviationInterventionPolicy()
    }

    @Test
    fun evaluate_whenZBelowAskThreshold_shouldNotAskOrTrigger() {
        val result = policy.evaluate(
            z = 1.9,
            intensity = "medium",
            alreadyAskedThisSession = false
        )

        assertEquals(1.9, result.zScore, 0.0001)
        assertFalse(result.shouldAskUsageCheck)
        assertFalse(result.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenZReachesAskThreshold_andNotAlreadyAsked_shouldAskButNotTriggerForMedium() {
        val result = policy.evaluate(
            z = 2.0,
            intensity = "medium",
            alreadyAskedThisSession = false
        )

        assertEquals(2.0, result.zScore, 0.0001)
        assertTrue(result.shouldAskUsageCheck)
        assertFalse(result.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenAlreadyAskedThisSession_shouldNotAskAgain() {
        val result = policy.evaluate(
            z = 2.8,
            intensity = "medium",
            alreadyAskedThisSession = true
        )

        assertEquals(2.8, result.zScore, 0.0001)
        assertFalse(result.shouldAskUsageCheck)
        assertTrue(result.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenLowIntensity_shouldTriggerOnlyAtOrAboveThree() {
        val belowThreshold = policy.evaluate(
            z = 2.99,
            intensity = "low",
            alreadyAskedThisSession = false
        )

        val atThreshold = policy.evaluate(
            z = 3.0,
            intensity = "low",
            alreadyAskedThisSession = false
        )

        assertFalse(belowThreshold.shouldTriggerIntervention)
        assertTrue(atThreshold.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenMediumIntensity_shouldTriggerAtOrAboveTwoPointFive() {
        val belowThreshold = policy.evaluate(
            z = 2.49,
            intensity = "medium",
            alreadyAskedThisSession = false
        )

        val atThreshold = policy.evaluate(
            z = 2.5,
            intensity = "medium",
            alreadyAskedThisSession = false
        )

        assertFalse(belowThreshold.shouldTriggerIntervention)
        assertTrue(atThreshold.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenHighIntensity_shouldTriggerAtOrAboveTwoPointThree() {
        val belowThreshold = policy.evaluate(
            z = 2.29,
            intensity = "high",
            alreadyAskedThisSession = false
        )

        val atThreshold = policy.evaluate(
            z = 2.3,
            intensity = "high",
            alreadyAskedThisSession = false
        )

        assertFalse(belowThreshold.shouldTriggerIntervention)
        assertTrue(atThreshold.shouldTriggerIntervention)
    }

    @Test
    fun evaluate_whenIntensityIsUnknown_shouldFallbackToMediumThreshold() {
        val belowMediumThreshold = policy.evaluate(
            z = 2.49,
            intensity = "unexpected-value",
            alreadyAskedThisSession = false
        )

        val atMediumThreshold = policy.evaluate(
            z = 2.5,
            intensity = "unexpected-value",
            alreadyAskedThisSession = false
        )

        assertFalse(belowMediumThreshold.shouldTriggerIntervention)
        assertTrue(atMediumThreshold.shouldTriggerIntervention)
    }

    @Test
    fun shouldResetAskState_whenZBelowRecoveryThreshold_shouldReturnTrue() {
        assertTrue(policy.shouldResetAskState(1.49))
    }

    @Test
    fun shouldResetAskState_whenZAtRecoveryThreshold_shouldReturnFalse() {
        assertFalse(policy.shouldResetAskState(1.5))
    }

    @Test
    fun shouldResetAskState_whenZAboveRecoveryThreshold_shouldReturnFalse() {
        assertFalse(policy.shouldResetAskState(1.8))
    }
}