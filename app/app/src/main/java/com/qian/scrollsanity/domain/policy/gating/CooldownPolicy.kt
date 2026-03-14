package com.qian.scrollsanity.domain.policy.gating

class CooldownPolicy {
    fun cooldownMs(intensity: String): Long = when (intensity.lowercase()) {
        "low" -> 20 * 60_000L
        "medium" -> 12 * 60_000L
        "high" -> 7 * 60_000L
        else -> 12 * 60_000L
    }
}