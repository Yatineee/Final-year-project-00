// ==============================
// data/TrackedApps.kt
// ==============================
package com.qian.scrollsanity.data

/**
 * Single source of truth for the 7 tracked apps.
 * Preferences store these IDs as strings (enum name), e.g. "YOUTUBE".
 */
enum class TrackedAppId {
    TIKTOK,
    YOUTUBE,
    INSTAGRAM,
    SNAPCHAT,
    X_TWITTER,
    FACEBOOK,
    REDDIT
}

data class TrackedAppMeta(
    val id: TrackedAppId,
    val displayName: String,
    val exactPackages: Set<String>,
    val fallbackContains: Set<String> = emptySet()
)

object TrackedApps {
    val all: List<TrackedAppMeta> = listOf(
        TrackedAppMeta(
            id = TrackedAppId.TIKTOK,
            displayName = "TikTok",
            exactPackages = setOf("com.zhiliaoapp.musically"),
            fallbackContains = setOf("musically", "tiktok")
        ),
        TrackedAppMeta(
            id = TrackedAppId.YOUTUBE,
            displayName = "YouTube",
            exactPackages = setOf("com.google.android.youtube"),
            fallbackContains = setOf("youtube")
        ),
        TrackedAppMeta(
            id = TrackedAppId.INSTAGRAM,
            displayName = "Instagram",
            exactPackages = setOf("com.instagram.android"),
            fallbackContains = setOf("instagram")
        ),
        TrackedAppMeta(
            id = TrackedAppId.SNAPCHAT,
            displayName = "Snapchat",
            exactPackages = setOf("com.snapchat.android"),
            fallbackContains = setOf("snapchat")
        ),
        TrackedAppMeta(
            id = TrackedAppId.X_TWITTER,
            displayName = "X (Twitter)",
            exactPackages = setOf("com.twitter.android"),
            fallbackContains = setOf("twitter", "x")
        ),
        TrackedAppMeta(
            id = TrackedAppId.FACEBOOK,
            displayName = "Facebook",
            exactPackages = setOf("com.facebook.katana"),
            fallbackContains = setOf("facebook")
        ),
        TrackedAppMeta(
            id = TrackedAppId.REDDIT,
            displayName = "Reddit",
            exactPackages = setOf("com.reddit.frontpage"),
            fallbackContains = setOf("reddit")
        )
    )

    val allIds: Set<TrackedAppId> = all.map { it.id }.toSet()

    fun metaFor(id: TrackedAppId): TrackedAppMeta =
        all.first { it.id == id }

    fun defaultEnabledIdStrings(): Set<String> =
        allIds.map { it.name }.toSet()
}
