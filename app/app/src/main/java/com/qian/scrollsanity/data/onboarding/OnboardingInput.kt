package com.qian.scrollsanity.data.onboarding

/**
 * OnboardingInput
 *
 * Used when completing onboarding.
 * These values initialize user profile, preferences,
 * and optionally seed goals / interests collections.
 */
data class OnboardingInput(

    // user profile
    val nickname: String,
    val displayName: String? = null,

    // preference configuration
    val toneStyle: String = "gentle",
    val interventionIntensity: String = "MEDIUM",

    // context used for prompt generation
    val recentGoalContext: String? = null,
    val recentInterestContext: String? = null,

    // initial lists created during onboarding
    val goals: List<String> = emptyList(),
    val interests: List<String> = emptyList()
)