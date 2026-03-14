package com.qian.scrollsanity.domain.model.session

sealed class PromptEvent {
    data class AskUsageType(val z: Double) : PromptEvent()
    // 后续你会加：AskMentalState / AskEngaging / ShowInterventionMessage ...
}