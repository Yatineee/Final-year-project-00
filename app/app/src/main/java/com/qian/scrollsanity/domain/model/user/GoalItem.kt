package com.qian.scrollsanity.domain.model.user

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * GoalItem (Firestore: users/{uid}/goals/{goalId})
 *
 * createdAtMs 用于稳定排序（客户端写入的 Long）
 * createdAt / achievedAt 仍保留 serverTimestamp 用于审计/展示
 */
data class GoalItem(
    @DocumentId
    val id: String = "",

    val text: String = "",
    val status: String = "active", // "active" or "achieved"

    // ✅ NEW: stable ordering field
    val createdAtMs: Long = 0L,

    @ServerTimestamp
    val createdAt: Date? = null,

    @ServerTimestamp
    val achievedAt: Date? = null
)