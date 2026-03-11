package com.qian.scrollsanity.data

/**
 * OnboardingInput
 *
 * 说明：
 * - 新字段用于 ScrollSanity 新设计（推荐使用）
 * - 旧字段暂时保留，避免现有 Onboarding UI/VM 立刻报错
 * - 你完成 UI 迁移后，可以删掉 legacy 区域
 */
data class OnboardingInput(

    // =========================
    // NEW (preferred)
    // =========================
    val nickname: String,
    val displayName: String? = null,

    // 用户最近目标（描述性文本，如“准备雅思/期末考试”）
    val recentGoalContext: String? = null,

    // 兴趣爱好（非使用习惯）
    val interests: List<String> = emptyList(),

    // 干预语气
    val toneStyle: String = "gentle",

    // 干预强度
    val interventionIntensity: String = "MEDIUM",

    // 方案 A：onboarding 可选 seed 一个 goal 到 goals 子集合
    val seedGoalText: String? = null,

    // =========================
    // LEGACY (temporary)
    // =========================
    // 旧版字段：先留着不让旧代码崩
    val habit: String = "",
    val goal: String = "",
    val recentChallenge: String = "",
    val preferStyle: String = "gentle"
)