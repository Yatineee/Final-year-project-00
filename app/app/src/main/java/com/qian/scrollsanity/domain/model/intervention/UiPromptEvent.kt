package com.qian.scrollsanity.domain.model.intervention

sealed class UiPromptEvent {
    object AskUsageType : UiPromptEvent()
    object AskMentalStateCategory : UiPromptEvent() // bored/stress/inertia
    object AskEngaging : UiPromptEvent() // yes/no
    data class ShowInterventionMessage(val text: String) : UiPromptEvent()
}