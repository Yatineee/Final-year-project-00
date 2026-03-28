package com.qian.scrollsanity.blocker.bus

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionEventBusTest {

    @Test
    fun `emits mental state decided event`() = runBlocking {
        val expected = InterventionEventBus.Event.MentalStateDecided(
            pkg = "com.example.app",
            mentalState = "stress",
            engaging = true,
            z = 3.2
        )

        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1000) {
                InterventionEventBus.events.first()
            }
        }

        InterventionEventBus.emit(expected)

        val actual = deferred.await() as InterventionEventBus.Event.MentalStateDecided

        assertEquals(expected.pkg, actual.pkg)
        assertEquals(expected.mentalState, actual.mentalState)
        assertEquals(expected.engaging, actual.engaging)
        assertEquals(expected.z, actual.z, 0.0001)
    }

    @Test
    fun `emits usage type decided event`() = runBlocking {
        val expected = InterventionEventBus.Event.UsageTypeDecided(
            pkg = "com.example.app",
            usageType = "HABIT",
            z = 2.5
        )

        val deferred = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1000) {
                InterventionEventBus.events.first()
            }
        }

        InterventionEventBus.emit(expected)

        val actual = deferred.await() as InterventionEventBus.Event.UsageTypeDecided

        assertEquals(expected.pkg, actual.pkg)
        assertEquals(expected.usageType, actual.usageType)
        assertEquals(expected.z, actual.z, 0.0001)
    }
}