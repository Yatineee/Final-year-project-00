package com.qian.scrollsanity.domain.model.usagedata

import com.qian.scrollsanity.data.TrackedAppId

/**
 * Represents a single continuous usage session of a tracked app.
 *
 * A session starts when the app moves to foreground
 * and ends when it leaves foreground (background, screen off, etc).
 */
data class AppSession(
    val trackedAppId: TrackedAppId,
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long,
    val durationMinutes: Int
)