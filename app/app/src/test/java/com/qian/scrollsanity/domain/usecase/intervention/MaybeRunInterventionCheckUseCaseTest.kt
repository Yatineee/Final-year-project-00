//package com.qian.scrollsanity.domain.usecase.intervention
//
//import com.qian.scrollsanity.data.usagedata.TrackedAppId
//import com.qian.scrollsanity.data.usagedata.TrackedApps
//import com.qian.scrollsanity.domain.model.usagedata.AppSession
//import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
//import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
//import com.qian.scrollsanity.domain.repo.LocalUsageRepo
//import com.qian.scrollsanity.domain.trigger.InterventionSessionState
//import kotlinx.coroutines.runBlocking
//import org.junit.Assert.assertEquals
//import org.junit.Assert.assertFalse
//import org.junit.Assert.assertTrue
//import org.junit.Before
//import org.junit.Test
//
//class MaybeRunInterventionCheckUseCaseTest {
//
//    private lateinit var useCase: MaybeRunInterventionCheckUseCase
//
//    private lateinit var fakeUsageRepo: FakeUsageRepo
//    private lateinit var fakeIntensityProvider: FakeIntensityProvider
//    private lateinit var fakeEnabledProvider: FakeEnabledProvider
//
//    /**
//     * Used in tests where cooldown should definitely be due.
//     *
//     * medium cooldown = 12 * 60_000 = 720_000 ms
//     */
//    private val dueNow = 1_000_000L
//
//    @Before
//    fun setup() {
//        fakeUsageRepo = FakeUsageRepo()
//        fakeIntensityProvider = FakeIntensityProvider("medium")
//        fakeEnabledProvider = FakeEnabledProvider(setOf(TrackedAppId.YOUTUBE))
//
//        useCase = MaybeRunInterventionCheckUseCase(
//            cooldownPolicy = CooldownPolicy(),
//            deviationPolicy = DeviationInterventionPolicy(),
//            intensityProvider = fakeIntensityProvider,
//            enabledProvider = fakeEnabledProvider,
//            localUsageRepo = fakeUsageRepo,
//            recordDashboardMetricsUseCase = RecordDashboardMetricsUseCase
//        )
//    }
//
//    @Test
//    fun `returns none when not in blocked session`() = runBlocking {
//        val state = InterventionSessionState(
//            inBlockedSession = false
//        )
//
//        val (_, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 10
//        )
//
//        assertTrue(result is TriggerResult.None)
//    }
//
//    @Test
//    fun `returns none when cooldown has not passed`() = runBlocking {
//        val now = System.currentTimeMillis()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = now
//        )
//
//        val (_, result) = useCase.run(
//            nowMs = now,
//            state = state,
//            currentSessionMinutes = 10
//        )
//
//        assertTrue(result is TriggerResult.None)
//    }
//
//    @Test
//    fun `returns none and updates lastCheckAtMs when no sessions are found`() = runBlocking {
//        fakeUsageRepo.sessions = emptyList()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 10
//        )
//
//        assertTrue(result is TriggerResult.None)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    @Test
//    fun `asks usage type when z is above ask threshold`() = runBlocking {
//        fakeUsageRepo.sessions = generateBaselineSessions()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L,
//            askedUsageTypeThisSession = false
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 200
//        )
//
//        assertTrue(result is TriggerResult.AskUsageType)
//        assertTrue(newState.askedUsageTypeThisSession)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    @Test
//    fun `triggers intervention with exact z value when deviation is very high and usage type was already asked`() = runBlocking {
//        fakeUsageRepo.sessions = generateBaselineSessions()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L,
//            askedUsageTypeThisSession = true
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 300
//        )
//
//        val trigger = result as TriggerResult.TriggerIntervention
//
//        // baseline durations = [9, 10, 10, 11, 12]
//        // median = 10, mad = 1
//        // z = (300 - 10) / (1 + 1) = 145.0
//        assertEquals(145.0, trigger.z, 0.0001)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//        assertTrue(newState.askedUsageTypeThisSession)
//    }
//
//    @Test
//    fun `resets askedUsageTypeThisSession when z has recovered`() = runBlocking {
//        fakeUsageRepo.sessions = generateBaselineSessions()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L,
//            askedUsageTypeThisSession = true
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 1
//        )
//
//        // z = (1 - 10) / (1 + 1) = -4.5
//        assertTrue(result is TriggerResult.None)
//        assertFalse(newState.askedUsageTypeThisSession)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    @Test
//    fun `ignores sessions shorter than one minute`() = runBlocking {
//        fakeUsageRepo.sessions = listOf(
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 0),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 10),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 12),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 11),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 9)
//        )
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L,
//            askedUsageTypeThisSession = false
//        )
//
//        val (_, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 200
//        )
//
//        assertTrue(result is TriggerResult.AskUsageType)
//    }
//
//    @Test
//    fun `returns none when all sessions are filtered out for being shorter than one minute`() = runBlocking {
//        fakeUsageRepo.sessions = listOf(
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 0),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 0)
//        )
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 50
//        )
//
//        assertTrue(result is TriggerResult.None)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    @Test
//    fun `returns none without resetting asked state when z is moderate`() = runBlocking {
//        fakeUsageRepo.sessions = generateBaselineSessions()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L,
//            askedUsageTypeThisSession = true
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 13
//        )
//
//        // z = (13 - 10) / (1 + 1) = 1.5
//        assertTrue(result is TriggerResult.None)
//        assertTrue(newState.askedUsageTypeThisSession)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    @Test
//    fun `requests recent sessions with exact enabled packages and seven day window`() = runBlocking {
//        fakeUsageRepo.sessions = generateBaselineSessions()
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L
//        )
//
//        useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 10
//        )
//
//        val expectedPackages = TrackedApps.metaFor(TrackedAppId.YOUTUBE).exactPackages.toSet()
//
//        assertEquals(expectedPackages, fakeUsageRepo.lastEnabledPackages)
//        assertEquals(7, fakeUsageRepo.lastDays)
//    }
//
//    @Test
//    fun `normalizes intensity from provider before applying cooldown policy`() = runBlocking {
//        fakeUsageRepo = FakeUsageRepo()
//        fakeIntensityProvider = FakeIntensityProvider(" MEDIUM ")
//        fakeEnabledProvider = FakeEnabledProvider(setOf(TrackedAppId.YOUTUBE))
//
//        useCase = MaybeRunInterventionCheckUseCase(
//            cooldownPolicy = CooldownPolicy(),
//            deviationPolicy = DeviationInterventionPolicy(),
//            intensityProvider = fakeIntensityProvider,
//            enabledProvider = fakeEnabledProvider,
//            localUsageRepo = fakeUsageRepo
//        )
//
//        val state = InterventionSessionState(
//            inBlockedSession = true,
//            lastCheckAtMs = 0L
//        )
//
//        val (newState, result) = useCase.run(
//            nowMs = dueNow,
//            state = state,
//            currentSessionMinutes = 10
//        )
//
//        assertTrue(result is TriggerResult.None)
//        assertEquals(dueNow, newState.lastCheckAtMs)
//    }
//
//    private fun generateBaselineSessions(): List<AppSession> {
//        return listOf(
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 10),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 12),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 11),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 9),
//            AppSession(TrackedAppId.YOUTUBE, "pkg", 0, 0, 10)
//        )
//    }
//}
//
//class FakeUsageRepo : LocalUsageRepo {
//
//    var sessions: List<AppSession> = emptyList()
//    var lastEnabledPackages: Set<String>? = null
//    var lastDays: Int? = null
//
//    override suspend fun getTodayTotalMinutes(enabled: Set<TrackedAppId>): Int = 0
//
//    override suspend fun getLast7DaysTotalMinutes(enabled: Set<TrackedAppId>): List<Int> = emptyList()
//
//    override suspend fun getRecentSessions(
//        enabledPackages: Set<String>,
//        days: Int
//    ): List<AppSession> {
//        lastEnabledPackages = enabledPackages
//        lastDays = days
//        return sessions
//    }
//}
//
//class FakeIntensityProvider(
//    private val value: String
//) : IntensityProvider {
//    override suspend fun getIntensity(): String = value
//}
//
//class FakeEnabledProvider(
//    private val ids: Set<TrackedAppId>
//) : EnabledTrackedProvider {
//    override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> = ids
//}