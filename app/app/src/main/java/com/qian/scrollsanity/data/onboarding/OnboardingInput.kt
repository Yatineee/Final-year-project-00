package com.qian.scrollsanity.data.onboarding

/**
 * OnboardingInput
 *
 * Used when completing onboarding.
 * These values initialize:
 * - user profile
 * - user preferences
 * - initial goals collection
 * - initial interests collection
 *
 * Notes:
 * - Goal and interest context should now be derived from the actual
 *   goals/interests collections rather than stored as separate primary fields.
 */
data class OnboardingInput(

    // User profile
    val nickname: String,
    val displayName: String? = null,

    // Preference configuration
    val toneStyle: String = "gentle",
    val interventionIntensity: String = "MEDIUM",

    // Initial collections created during onboarding
    val goals: List<String> = emptyList(),
    val interests: List<String> = emptyList()
)