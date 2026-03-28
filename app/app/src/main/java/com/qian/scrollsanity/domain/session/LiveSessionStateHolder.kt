package com.qian.scrollsanity.domain.session

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LiveSessionStateHolder {

    data class State(
        val trackedAppId: TrackedAppId? = null,
        val packageName: String? = null,
        val inTrackedSession: Boolean = false,
        val sessionStartElapsedMs: Long = 0L,
        val currentSessionMinutes: Int = 0
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    fun startSession(
        appId: TrackedAppId,
        packageName: String,
        startElapsedMs: Long
    ) {
        _state.value = State(
            trackedAppId = appId,
            packageName = packageName,
            inTrackedSession = true,
            sessionStartElapsedMs = startElapsedMs,
            currentSessionMinutes = 0
        )
    }

    fun endSession() {
        _state.value = State()
    }

    fun updateMinutes(nowElapsedMs: Long) {
        val current = _state.value
        if (!current.inTrackedSession) return

        val minutes = ((nowElapsedMs - current.sessionStartElapsedMs) / 60_000).toInt()
        if (minutes != current.currentSessionMinutes) {
            _state.value = current.copy(currentSessionMinutes = minutes)
        }
    }
}