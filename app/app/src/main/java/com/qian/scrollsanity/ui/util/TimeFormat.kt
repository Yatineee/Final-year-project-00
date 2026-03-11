package com.qian.scrollsanity.ui.util

import kotlin.math.abs

fun formatTime(minutes: Int): String {
    val absMinutes = abs(minutes)
    val h = absMinutes / 60
    val m = absMinutes % 60
    val prefix = if (minutes < 0) "-" else ""
    return if (h > 0) "${prefix}${h}h ${m}m" else "${prefix}${m}m"
}
