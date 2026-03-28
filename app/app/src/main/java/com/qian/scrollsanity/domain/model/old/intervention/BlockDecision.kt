// app/src/main/java/com/hamzah/arete/domain/model/BlockDecision.kt
package com.qian.scrollsanity.domain.model.old.intervention

sealed class BlockDecision {
    data object Allow : BlockDecision()

    data class Block(
        val packageName: String,
        val reason: BlockReason,
        val usedMinutes: Int,
        val limitMinutes: Int
    ) : BlockDecision()
}

enum class BlockReason {
    DailyLimitReached,
    FocusSessionRestricted
}
