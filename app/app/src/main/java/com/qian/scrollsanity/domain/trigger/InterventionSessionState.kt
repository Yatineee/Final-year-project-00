package com.qian.scrollsanity.domain.trigger

data class InterventionSessionState(
    val inBlockedSession: Boolean = false,
    val currentPackage: String? = null,
    val lastCheckAtMs: Long = 0L,
    val askedUsageTypeThisSession: Boolean = false
)