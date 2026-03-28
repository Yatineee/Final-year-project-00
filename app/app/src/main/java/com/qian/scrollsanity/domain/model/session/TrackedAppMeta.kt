package com.qian.scrollsanity.domain.model.session

data class TrackedAppMeta(
    val id: TrackedAppId,
    val displayName: String,
    val exactPackages: Set<String>,
    val fallbackContains: Set<String> = emptySet()
)