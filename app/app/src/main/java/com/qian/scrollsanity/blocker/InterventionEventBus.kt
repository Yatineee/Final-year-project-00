package com.qian.scrollsanity.blocker

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object InterventionEventBus {

    sealed class Event {

        data class UsageTypeDecided(
            val pkg: String,
            val usageType: String,
            val z: Double
        ) : Event()

        data class MentalStateDecided(
            val pkg: String,
            val mentalState: String,
            val engaging: Boolean,
            val z: Double
        ) : Event()
    }

    private val _events = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 8
    )

    val events: SharedFlow<Event> = _events

    fun emit(event: Event) {
        // tryEmit avoids suspending; if buffer full, event may drop (acceptable for UI choice)
        _events.tryEmit(event)
    }
}