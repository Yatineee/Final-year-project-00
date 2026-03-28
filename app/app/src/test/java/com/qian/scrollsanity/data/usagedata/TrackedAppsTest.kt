package com.qian.scrollsanity.data.usagedata

import com.qian.scrollsanity.domain.util.TrackedAppId
import com.qian.scrollsanity.domain.util.TrackedAppMeta
import com.qian.scrollsanity.domain.util.TrackedApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackedAppsTest {

    @Test
    fun all_contains_all_7_tracked_apps() {
        assertEquals(7, TrackedApps.all.size)
        assertEquals(7, TrackedApps.allIds.size)

        val expectedIds = setOf(
            TrackedAppId.TIKTOK,
            TrackedAppId.YOUTUBE,
            TrackedAppId.INSTAGRAM,
            TrackedAppId.SNAPCHAT,
            TrackedAppId.X_TWITTER,
            TrackedAppId.FACEBOOK,
            TrackedAppId.REDDIT
        )

        assertEquals(expectedIds, TrackedApps.allIds)
    }

    @Test
    fun metaFor_returns_correct_meta_for_youtube() {
        val meta = TrackedApps.metaFor(TrackedAppId.YOUTUBE)

        assertEquals(TrackedAppId.YOUTUBE, meta.id)
        assertEquals("YouTube", meta.displayName)
        assertEquals(setOf("com.google.android.youtube"), meta.exactPackages)
        assertEquals(setOf("youtube"), meta.fallbackContains)
    }

    @Test
    fun metaFor_returns_correct_meta_for_x_twitter() {
        val meta = TrackedApps.metaFor(TrackedAppId.X_TWITTER)

        assertEquals(TrackedAppId.X_TWITTER, meta.id)
        assertEquals("X (Twitter)", meta.displayName)
        assertEquals(setOf("com.twitter.android"), meta.exactPackages)
        assertEquals(setOf("twitter", "x"), meta.fallbackContains)
    }

    @Test
    fun allIds_matches_ids_derived_from_all() {
        val idsFromAll = TrackedApps.all.map { it.id }.toSet()

        assertEquals(idsFromAll, TrackedApps.allIds)
    }

    @Test
    fun defaultEnabledIdStrings_returns_all_enum_names() {
        val expected = setOf(
            "TIKTOK",
            "YOUTUBE",
            "INSTAGRAM",
            "SNAPCHAT",
            "X_TWITTER",
            "FACEBOOK",
            "REDDIT"
        )

        val actual = TrackedApps.defaultEnabledIdStrings()

        assertEquals(expected, actual)
        assertEquals(TrackedApps.allIds.size, actual.size)
    }

    @Test
    fun all_meta_entries_have_non_blank_display_names_and_non_empty_packages() {
        TrackedApps.all.forEach { meta ->
            assertTrue(meta.displayName.isNotBlank())
            assertTrue(meta.exactPackages.isNotEmpty())
        }
    }

    @Test
    fun all_meta_entries_have_unique_ids_display_names_and_exact_packages() {
        val ids = TrackedApps.all.map { it.id }
        val displayNames = TrackedApps.all.map { it.displayName }
        val allExactPackages = TrackedApps.all.flatMap { it.exactPackages }

        assertEquals(ids.size, ids.toSet().size)
        assertEquals(displayNames.size, displayNames.toSet().size)
        assertEquals(allExactPackages.size, allExactPackages.toSet().size)
    }

    @Test
    fun fallbackContains_can_be_empty_but_not_blank_when_present() {
        TrackedApps.all.forEach { meta ->
            meta.fallbackContains.forEach { fallback ->
                assertFalse(fallback.isBlank())
            }
        }
    }

    @Test
    fun all_matches_expected_catalog_definition() {
        val expected = listOf(
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

        assertEquals(expected, TrackedApps.all)
    }
}