package com.qian.scrollsanity.domain.policy.decision

data class DeviationResult(
    val zScore: Double,
    val shouldAskUsageCheck: Boolean,
    val shouldTriggerIntervention: Boolean
)

class DeviationInterventionPolicy {

    fun evaluate(
        z: Double,
        intensity: String,
        alreadyAskedThisSession: Boolean
    ): DeviationResult {

        val shouldAskUsageCheck = z >= ASK_CHECK_THRESHOLD && !alreadyAskedThisSession
        val shouldTriggerIntervention = z >= interventionThreshold(intensity)

        return DeviationResult(
            zScore = z,
            shouldAskUsageCheck = shouldAskUsageCheck,
            shouldTriggerIntervention = shouldTriggerIntervention
        )
    }

    fun shouldResetAskState(z: Double): Boolean {
        return z < RECOVERY_THRESHOLD
    }

    fun interventionThreshold(intensity: String): Double {
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