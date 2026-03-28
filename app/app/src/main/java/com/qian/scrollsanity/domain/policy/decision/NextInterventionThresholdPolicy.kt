package com.qian.scrollsanity.domain.policy.decision

class NextInterventionThresholdPolicy {

    fun computeNextThreshold(
        intensity: String,
        triggeredZ: Double
    ): Double {
        val baseThreshold = when (intensity.lowercase()) {
            "low" -> 3.0
            "medium" -> 2.5
            "high" -> 2.3
            else -> 2.5
        }

        val increment = when (intensity.lowercase()) {
            "low" -> 0.40
            "medium" -> 0.25
            "high" -> 0.15
            else -> 0.25
        }

        return maxOf(baseThreshold, triggeredZ + increment)
    }
}