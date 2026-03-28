package com.qian.scrollsanity.domain.model.old.session

sealed class PromptEvent {
    data class AskUsageType(val z: Double) : PromptEvent()
    // 后续你会加：AskMentalState / AskEngaging / ShowInterventionMessage ...
}