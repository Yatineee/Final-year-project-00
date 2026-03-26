package com.qian.scrollsanity.domain.usecase.intervention

import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.domain.model.usagedata.AppSession
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MaybeRunInterventionCheckUseCaseTest {

    private lateinit var useCase: MaybeRunInterventionCheckUseCase

    private lateinit var fakeUsageRepo: FakeUsageRepo
    private lateinit var fakeIntensityProvider: FakeIntensityProvider
    private lateinit var fakeEnabledProvider: FakeEnabledProvider

    /**
     * Make sure cooldown is always due in tests that are NOT specifically
     * testing cooldown behaviour.
     *
     * medium cooldown = 12 * 60_000 = 720_000 ms
     */
    private val dueNow = 1_000_000L

    @Before
    fun setup() {
        fakeUsageRepo = FakeUsageRepo()
        fakeIntensityProvider = FakeIntensityProvider("medium")
        fakeEnabledProvider = FakeEnabledProvider(setOf(TrackedAppId.YOUTUBE))

        useCase = MaybeRunInterventionCheckUseCase(
            cooldownPolicy = CooldownPolicy(),
            deviationPolicy = DeviationInterventionPolicy(),
            intensityProvider = fakeIntensityProvider,
            enabledProvider = fakeEnabledProvider,
            localUsageRepo = fakeUsageRepo
        )
    }

    @Test
    fun run_whenNotInBlockedSession_shouldReturnNone() = runBlocking {
        val state = InterventionSessionState(
            inBlockedSession = false
        )

        val (_, result) = useCase.run(
            nowMs = dueNow,
            state = state,
            currentSessionMinutes = 10
        )

        assertTrue(result is TriggerResult.None)
    }

    @Test
    fun run_whenCooldownNotPassed_shouldReturnNone() = runBlocking {
        val now = System.currentTimeMillis()

        val state = InterventionSessionState(
            inBlockedSession = true,
            lastCheckAtMs = now
        )

        val (_, result) = useCase.run(
            nowMs = now,
            state = state,
            currentSessionMinutes = 10
        )

        assertTrue(result is TriggerResult.None)
    }

    @Test
    fun run_whenNoSessions_shouldReturnNoneAndUpdateState() = runBlocking {
        fakeUsageRepo.sessions = emptyList()

        val state = InterventionSessionState(
            inBlockedSession = true,
            lastCheckAtMs = 0L
        )

        val (newState, result) = useCase.run(
            nowMs = dueNow,
            state = state,
            currentSessionMinutes = 10
        )

        assertTrue(result is TriggerResult.None)
        assertEquals(dueNow, newState.lastCheckAtMs)
    }

    @Test
    fun run_whenZAboveAskThreshold_shouldAskUsageType() = runBlocking {
        fakeUsageRepo.sessions = generateBaselineSessions()

        val state = InterventionSessionState(
            inBlockedSession = true,
            lastCheckAtMs = 0L,
            askedUsageTypeThisSession = false
        )

        val (newState, result) = useCase.run(
            nowMs = dueNow,
            state = state,
            currentSessionMinutes = 200
        )

        assertTrue(result is TriggerResult.AskUsageType)
        assertTrue(newState.askedUsageTypeThisSession)
        assertEquals(dueNow, newState.lastCheckAtMs)
    }

    @Test
    fun run_whenZVeryHigh_andAlreadyAsked_shouldTriggerIntervention() = runBlocking {
        fakeUsageRepo.sessions = generateBaselineSessions()

        val state = InterventionSessionState(
            inBlockedSession = true,
            lastCheckAtMs = 0L,
            askedUsageTypeThisSession = true
        )

        val (newState, result) = useCase.run(
            nowMs = dueNow,
            state = state,
            currentSessionMinutes = 300
        )

        assertTrue(result is TriggerResult.TriggerIntervention)
        assertEquals(dueNow, newState.lastCheckAtMs)
        assertTrue(newState.askedUsageTypeThisSession)
    }

    @Test
    fun run_whenZRecovered_shouldResetAskState() = runBlocking {
        fakeUsageRepo.sessions = generateBaselineSessions()

        val state = InterventionSessionState(
            inBlockedSession = true,
            lastCheckAtMs = 0L,
            askedUsageTypeThisSession = true
        )

        val (newState, result) = useCase.run(
            nowMs = dueNow,
            state = state,
            currentSessionMinutes = 1
        )

        assertTrue(result is TriggerResult.None)
        assertFalse(newState.askedUsageTypeThisSession)
        assertEquals(dueNow, newState.lastCheckAtMs)
    }

    private fun generateBaselineSessions(): List<AppSession> {
        return listOf(
            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 10),
            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 12),
            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 11),
            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 9),
            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 10)
        )
    }
}

class FakeUsageRepo : LocalUsageRepo {

    var sessions: List<AppSession> = emptyList()

    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int = 0

    override suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int> = emptyList()

    override suspend fun getRecentSessions(
        enabledPackages: Set<String>,
        days: Int
    ): List<AppSession> {
        return sessions
    }
}

class FakeIntensityProvider(
    private val value: String
) : IntensityProvider {
    override suspend fun getIntensity(): String = value
}

class FakeEnabledProvider(
    private val ids: Set<TrackedAppId>
) : EnabledTrackedProvider {
    override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> = ids
}