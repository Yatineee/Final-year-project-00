package com.qian.scrollsanity.domain.model.old.intervention

sealed class InterventionDecision {
    data object Allow : InterventionDecision()

    data class Intervene(
        val packageName: String,
        val reason: InterventionReason,
        val context: InterventionContext
    ) : InterventionDecision()
}

data class InterventionContext(
    // 先兼容现有“分钟限制”逻辑（后面你可以逐步替换成秒、z值等）
    val usedMinutes: Int = 0,
    val limitMinutes: Int = 0,

    // 预留你后续的核心字段（先默认值，不影响编译）
    val todaySec: Long = 0L,
    val zValue: Double = 0.0,
    val baselineMedianSec: Double = 0.0,
    val baselineMadSec: Double = 0.0,

    val intensity: InterventionIntensity = InterventionIntensity.MEDIUM,
    val cooldownRemainingSec: Long = 0L
)

enum class InterventionReason {
    DailyLimitReached,
    FocusSessionRestricted,

    // 你未来会加的：
    DeviationHigh
}

enum class InterventionIntensity { LOW, MEDIUM, HIGH }
