package com.qian.scrollsanity.domain.policy.app

class ScreenTimePolicy {
    fun isOverLimit(usedMinutes: Int, limitMinutes: Int): Boolean {
        if (limitMinutes <= 0) return false
        return usedMinutes >= limitMinutes
    }

    fun remainingMinutes(usedMinutes: Int, limitMinutes: Int): Int {
        if (limitMinutes <= 0) return Int.MAX_VALUE
        return (limitMinutes - usedMinutes).coerceAtLeast(0)
    }
}
