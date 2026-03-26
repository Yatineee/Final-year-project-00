package com.qian.scrollsanity.domain.policy.gating

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CooldownPolicyTest {

    private lateinit var policy: CooldownPolicy

    @Before
    fun setup() {
        policy = CooldownPolicy()
    }

    @Test
    fun cooldownMs_returns20Minutes_forLowIntensity() {
        val result = policy.cooldownMs("low")

        assertEquals(20 * 60_000L, result)
    }

    @Test
    fun cooldownMs_returns12Minutes_forMediumIntensity() {
        val result = policy.cooldownMs("medium")

        assertEquals(12 * 60_000L, result)
    }

    @Test
    fun cooldownMs_returns7Minutes_forHighIntensity() {
        val result = policy.cooldownMs("high")

        assertEquals(7 * 60_000L, result)
    }

    @Test
    fun cooldownMs_returnsDefault_forUnknownIntensity() {
        val result = policy.cooldownMs("unknown")

        assertEquals(12 * 60_000L, result)
    }

    @Test
    fun cooldownMs_isCaseInsensitive() {
        val result = policy.cooldownMs("LOW")

        assertEquals(20 * 60_000L, result)
    }
}