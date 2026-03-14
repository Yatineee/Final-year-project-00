package com.qian.scrollsanity.domain.model.user

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class InterestItem(
    @DocumentId
    val id: String = "",

    val text: String = "",
    val status: String = "active", // active / achieved

    val createdAtMs: Long = 0L,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val achievedAt: Date? = null
)