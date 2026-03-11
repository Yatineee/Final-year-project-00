package com.qian.scrollsanity.domain.policy

data class DeviationResult(
    val zScore: Double,
    val shouldAskUsageCheck: Boolean,
    val shouldTriggerIntervention: Boolean
)

class DeviationInterventionPolicy {

    private var hasAskedThisSession = false

    fun evaluate(
        z: Double,
        intensity: String
    ): DeviationResult {

        val askCheck = z >= 2 && !hasAskedThisSession

        if (askCheck) {
            hasAskedThisSession = true
        }

        val triggerThreshold = when (intensity.lowercase()) {
            "low" -> 3.0
            "medium" -> 2.5
            "high" -> 2.3
            else -> 2.5
        }

        val trigger = z >= triggerThreshold

        return DeviationResult(
            zScore = z,
            shouldAskUsageCheck = askCheck,
            shouldTriggerIntervention = trigger
        )
    }

    fun resetIfRecovered(z: Double) {
        if (z < 1.5) {
            hasAskedThisSession = false
        }
    }
}