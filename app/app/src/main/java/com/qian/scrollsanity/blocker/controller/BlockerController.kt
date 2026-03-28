package com.qian.scrollsanity.blocker.controller

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.qian.scrollsanity.blocker.ui.BlockerActivity
import com.qian.scrollsanity.data.dashboard.DashboardMetricsRepoImpl
import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.data.usagedata.TrackedAppId
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.data.usagedata.UsageStatsRepository
import com.qian.scrollsanity.domain.policy.decision.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.policy.gating.CooldownPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.dashboard.GetDashboardSummaryUseCase
import com.qian.scrollsanity.domain.usecase.dashboard.RecordDashboardMetricsUseCase
import com.qian.scrollsanity.domain.usecase.intervention.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.intervention.IntensityProvider
import com.qian.scrollsanity.domain.usecase.intervention.MaybeRunInterventionCheckUseCase
import com.qian.scrollsanity.domain.usecase.intervention.TriggerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Controller for foreground tracked-app blocking and intervention checks.
 *
 * Responsibilities:
 * - listen to foreground package changes
 * - maintain current intervention session state
 * - compute current session duration
 * - run deviation-based intervention check
 * - launch blocker UI based on TriggerResult
 *
 * Notes:
 * - session memory is stored in sessionState
 * - deviation policy is stateless and pure
 * - ask / trigger UI branching happens here
 */
object BlockerController {

    private const val TAG = "BlockerController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // =========================
    // Event throttles
    // =========================

    private var lastHandledPkg: String? = null
    private var lastHandledAtMs: Long = 0L
    private const val MIN_HANDLE_GAP_MS = 400L

    private var lastLaunchedAtMs: Long = 0L
    private const val MIN_LAUNCH_GAP_MS = 1200L

    // =========================
    // Session state
    // =========================

    private var sessionState = InterventionSessionState()
    private var currentSessionStartElapsedMs: Long? = null

    // =========================
    // Policy + use case dependencies
    // =========================

    private val cooldownPolicy = CooldownPolicy()
    private val deviationPolicy = DeviationInterventionPolicy()

    // =========================
    // BlockerActivity extras
    // =========================

    private const val EXTRA_MODE = "mode"
    private const val MODE_ASK_USAGE_TYPE = "ASK_USAGE_TYPE"
    private const val MODE_BLOCK = "BLOCK"

    private const val EXTRA_TARGET_PKG = "target_pkg"
    private const val EXTRA_BLOCKED_PKG = "blocked_pkg" // backward compatibility
    private const val EXTRA_Z = "z_score"
    private const val EXTRA_REASON = "reason"

    fun onAccessibilityConnected(context: Context) {
        Log.d(TAG, "Accessibility service connected")
    }

    /**
     * Called whenever the foreground package changes.
     */
    fun onForegroundPackageChanged(context: Context, packageName: String) {
        // Ignore our own app to avoid blocker self-loop
        if (packageName.startsWith(context.packageName)) return

        val now = SystemClock.elapsedRealtime()

        // Throttle noisy repeated accessibility events
        if (packageName == lastHandledPkg && (now - lastHandledAtMs) < MIN_HANDLE_GAP_MS) {
            return
        }
        lastHandledPkg = packageName
        lastHandledAtMs = now

        scope.launch {
            try {
                val prefsManager = PreferencesManager(context)

                val enabledIds: Set<TrackedAppId> = prefsManager.enabledTrackedApps.first()
                val enabledPackages: Set<String> =
                    enabledIds.flatMap { id -> TrackedApps.metaFor(id).exactPackages }.toSet()

                val isTracked = packageName in enabledPackages

                if (!isTracked) {
                    if (sessionState.inBlockedSession) {
                        Log.d(TAG, "Leaving tracked session, reset state")
                    }

                    currentSessionStartElapsedMs = null
                    sessionState = sessionState.copy(
                        inBlockedSession = false,
                        currentPackage = null,
                        lastCheckAtMs = 0L,
                        askedUsageTypeThisSession = false
                    )
                    return@launch
                }

                // Entering a new tracked session
                if (!sessionState.inBlockedSession || sessionState.currentPackage != packageName) {
                    Log.d(TAG, "Entering tracked session: $packageName")

                    currentSessionStartElapsedMs = now
                    sessionState = sessionState.copy(
                        inBlockedSession = true,
                        currentPackage = packageName,
                        lastCheckAtMs = 0L,
                        askedUsageTypeThisSession = false
                    )
                }

                val useCase = buildUseCase(context)
                val sessionStart = currentSessionStartElapsedMs ?: now
                val currentSessionMinutes =
                    ((now - sessionStart) / 60_000L).toInt().coerceAtLeast(0)

                val oldState = sessionState

                val (newState, result) = useCase.run(
                    nowMs = now,
                    state = sessionState,
                    currentSessionMinutes = currentSessionMinutes
                )

                Log.d(
                    TAG,
                    "Intervention evaluated: oldState=$oldState, newState=$newState, result=$result, currentSessionMinutes=$currentSessionMinutes, packageName=$packageName"
                )

                sessionState = newState

                when (result) {
                    is TriggerResult.None -> {
                        Log.d(TAG, "No intervention action needed")
                    }

                    is TriggerResult.AskUsageType -> {
                        launchGapGuardOrSkip {
                            showAskUsageType(
                                context = context,
                                targetPkg = packageName,
                                zScore = 0.0
                            )
                        }
                    }

                    is TriggerResult.TriggerIntervention -> {
                        launchGapGuardOrSkip {
                            showInterventionPlaceholder(
                                context = context,
                                targetPkg = packageName,
                                zScore = result.z
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in onForegroundPackageChanged($packageName)", t)
            }
        }
    }

    private fun buildDashboardSummaryUseCase(context: Context): GetDashboardSummaryUseCase {
        val prefsManager = PreferencesManager(context)
        val usageRepoReal = UsageStatsRepository(context)
        val localUsageRepo: LocalUsageRepo = usageRepoReal
        val dashboardMetricsRepo = DashboardMetricsRepoImpl(context)

        val enabledProvider = object : EnabledTrackedProvider {
            override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
                return prefsManager.enabledTrackedApps.first()
            }
        }

        return GetDashboardSummaryUseCase(
            localUsageRepo = localUsageRepo,
            enabledTrackedProvider = enabledProvider,
            dashboardMetricsRepo = dashboardMetricsRepo
        )
    }

    /**
     * Builds the use case with runtime providers backed by preferences and usage repository.
     */
    private fun buildUseCase(context: Context): MaybeRunInterventionCheckUseCase {
        val prefsManager = PreferencesManager(context)
        val usageRepoReal = UsageStatsRepository(context)
        val localUsageRepo: LocalUsageRepo = usageRepoReal

        val dashboardMetricsRepo = DashboardMetricsRepoImpl(context)
        val recordDashboardMetricsUseCase =
            RecordDashboardMetricsUseCase(dashboardMetricsRepo)

        val intensityProvider = object : IntensityProvider {
            override suspend fun getIntensity(): String {
                return prefsManager.interventionIntensity.first()
            }
        }

        val enabledProvider = object : EnabledTrackedProvider {
            override suspend fun getEnabledTrackedIds(): Set<TrackedAppId> {
                return prefsManager.enabledTrackedApps.first()
            }
        }

        return MaybeRunInterventionCheckUseCase(
            cooldownPolicy = cooldownPolicy,
            deviationPolicy = deviationPolicy,
            intensityProvider = intensityProvider,
            enabledProvider = enabledProvider,
            localUsageRepo = localUsageRepo,
            recordDashboardMetricsUseCase = recordDashboardMetricsUseCase
        )
    }

    /**
     * Prevents blocker UI from being launched too frequently.
     */
    private inline fun launchGapGuardOrSkip(block: () -> Unit) {
        val launchNow = SystemClock.elapsedRealtime()
        if ((launchNow - lastLaunchedAtMs) < MIN_LAUNCH_GAP_MS) {
            Log.d(TAG, "Launch throttled (gap), skip showing UI")
            return
        }
        lastLaunchedAtMs = launchNow
        block()
    }

    /**
     * Launch usage-type question UI.
     */
    private fun showAskUsageType(context: Context, targetPkg: String, zScore: Double) {
        Log.d(TAG, "Show ASK_USAGE_TYPE for $targetPkg")

        val intent = Intent(context, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_ASK_USAGE_TYPE)
            putExtra(EXTRA_TARGET_PKG, targetPkg)
            putExtra(EXTRA_BLOCKED_PKG, targetPkg)
            putExtra(EXTRA_Z, zScore)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }

    /**
     * Launch intervention placeholder UI.
     */
    private fun showInterventionPlaceholder(context: Context, targetPkg: String, zScore: Double) {
        Log.d(TAG, "Show MODE_BLOCK placeholder for $targetPkg, z=$zScore")

        val reason = "z=${String.format("%.2f", zScore)}"

        val intent = Intent(context, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_BLOCK)
            putExtra(EXTRA_TARGET_PKG, targetPkg)
            putExtra(EXTRA_BLOCKED_PKG, targetPkg)
            putExtra(EXTRA_Z, zScore)
            putExtra(EXTRA_REASON, reason)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(intent)
    }
}