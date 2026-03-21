package com.qian.scrollsanity.domain.policy.decision

/**
 * Decision result for deviation-based intervention logic.
 *
 * @property zScore the deviation z-score used for this evaluation
 * @property shouldAskUsageCheck whether the system should ask the user for a usage check
 * @property shouldTriggerIntervention whether the system should trigger an intervention
 */
data class DeviationResult(
    val zScore: Double,
    val shouldAskUsageCheck: Boolean,
    val shouldTriggerIntervention: Boolean
)

/**
 * Pure decision policy for deviation-based intervention.
 *
 * Responsibility:
 * - Decide whether to ask for a usage check
 * - Decide whether to trigger an intervention
 *
 * This class is intentionally stateless:
 * - it does not store session memory
 * - it does not mutate internal fields
 * - same input always produces same output
 *
 * Session-related memory such as "has asked in this session" should be managed
 * by the use case layer through InterventionSessionState.
 */
class DeviationInterventionPolicy {

    /**
     * Evaluates deviation rules based on current z-score, intervention intensity,
     * and whether the user has already been asked in this session.
     *
     * @param z current deviation z-score
     * @param intensity configured intervention intensity
     * @param alreadyAskedThisSession whether usage check has already been asked in this session
     */
    fun evaluate(
        z: Double,
        intensity: String,
        alreadyAskedThisSession: Boolean
    ): DeviationResult {

        val shouldAskUsageCheck = z >= ASK_CHECK_THRESHOLD && !alreadyAskedThisSession
        val shouldTriggerIntervention = z >= triggerThresholdFor(intensity)

        return DeviationResult(
            zScore = z,
            shouldAskUsageCheck = shouldAskUsageCheck,
            shouldTriggerIntervention = shouldTriggerIntervention
        )
    }

    /**
     * Returns whether the current deviation is considered recovered enough
     * to clear session-level "already asked" memory.
     *
     * Note:
     * This method is still pure. It only expresses a rule and does not mutate state.
     * The actual reset action should be performed in the use case layer.
     */
    fun shouldResetAskState(z: Double): Boolean {
        return z < RECOVERY_THRESHOLD
    }

    /**
     * Maps intervention intensity to trigger threshold.
     */
    private fun triggerThresholdFor(intensity: String): Double {
        return when (intensity.lowercase()) {
            "low" -> 3.0
            "medium" -> 2.5
            "high" -> 2.3
            else -> 2.5
        }
    }

    companion object {
        private const val ASK_CHECK_THRESHOLD = 2.0
        private const val RECOVERY_THRESHOLD = 1.5
    }
}