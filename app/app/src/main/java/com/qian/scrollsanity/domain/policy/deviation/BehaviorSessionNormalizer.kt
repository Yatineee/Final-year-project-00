package com.qian.scrollsanity.domain.policy.deviation

import com.qian.scrollsanity.domain.model.session.AppSession

/**
 * Normalizes raw tracked-app sessions into behavior-level sessions.
 *
 * Rules:
 * 1. Drop sessions shorter than 1 minute
 * 2. Merge adjacent sessions if the gap between them is less than 10 minutes
 * 3. Merge across any tracked apps (not only the same app)
 *
 * Why:
 * - brief app switches (replying to messages, checking notifications, etc.)
 *   should not fragment one continuous scrolling episode into many tiny sessions
 */
object BehaviorSessionNormalizer {

    private const val MIN_SESSION_MINUTES = 1
    private const val MERGE_GAP_MINUTES = 10
    private const val MILLIS_PER_MINUTE = 60_000L

    fun normalize(sessions: List<AppSession>): List<AppSession> {
        if (sessions.isEmpty()) return emptyList()

        val filtered = sessions
            .filter { it.durationMinutes >= MIN_SESSION_MINUTES }
            .sortedBy { it.startMillis }

        if (filtered.isEmpty()) return emptyList()

        val merged = mutableListOf<AppSession>()
        var current = filtered.first()

        for (i in 1 until filtered.size) {
            val next = filtered[i]
            val gapMinutes = ((next.startMillis - current.endMillis) / MILLIS_PER_MINUTE).toInt()

            current = if (gapMinutes < MERGE_GAP_MINUTES) {
                merge(current, next)
            } else {
                merged.add(current)
                next
            }
        }

        merged.add(current)
        return merged
    }

    private fun merge(first: AppSession, second: AppSession): AppSession {
        val mergedStart = minOf(first.startMillis, second.startMillis)
        val mergedEnd = maxOf(first.endMillis, second.endMillis)
        val mergedDurationMinutes = ((mergedEnd - mergedStart) / MILLIS_PER_MINUTE).toInt()

        return AppSession(
            // Keep metadata from the first session as representative info.
            // For behavior-level sessions, timing is the important part.
            trackedAppId = first.trackedAppId,
            packageName = first.packageName,
            startMillis = mergedStart,
            endMillis = mergedEnd,
            durationMinutes = mergedDurationMinutes
        )
    }
}