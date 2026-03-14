package com.qian.scrollsanity.blocker

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.qian.scrollsanity.data.PreferencesManager
import com.qian.scrollsanity.data.TrackedAppId
import com.qian.scrollsanity.data.TrackedApps
import com.qian.scrollsanity.data.usagedata.UsageStatsRepository
import com.qian.scrollsanity.domain.policy.DeviationInterventionPolicy
import com.qian.scrollsanity.domain.repo.LocalUsageRepo
import com.qian.scrollsanity.domain.trigger.CooldownPolicy
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.EnabledTrackedProvider
import com.qian.scrollsanity.domain.usecase.IntensityProvider
import com.qian.scrollsanity.domain.usecase.MaybeRunInterventionCheckUseCase
import com.qian.scrollsanity.domain.usecase.TriggerResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BlockerController (battery-friendly)
 *
 * - Only checks when user is inside a tracked app (blocked session)
 * - Cooldown based on intensity: LOW 20m / MEDIUM 12m / HIGH 7m
 * - When z >= 2: ask "Intentional vs Habit" (MODE_ASK_USAGE_TYPE)
 * - When z >= threshold(intensity): trigger intervention placeholder (MODE_BLOCK)
 */
object BlockerController {

    private const val TAG = "BlockerController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ========== Throttles ==========
    private var lastHandledPkg: String? = null
    private var lastHandledAtMs: Long = 0L
    private const val MIN_HANDLE_GAP_MS = 400L

    private var lastLaunchedAtMs: Long = 0L
    private const val MIN_LAUNCH_GAP_MS = 1200L

    // ========== Session state ==========
    private var sessionState = InterventionSessionState()

    // policy + usecase wiring
    private val cooldownPolicy = CooldownPolicy()
    private val deviationPolicy = DeviationInterventionPolicy()

    // BlockerActivity extras (must match BlockerActivity companion)
    private const val EXTRA_MODE = "mode"
    private const val MODE_ASK_USAGE_TYPE = "ASK_USAGE_TYPE"
    private const val MODE_BLOCK = "BLOCK"

    private const val EXTRA_TARGET_PKG = "target_pkg"
    private const val EXTRA_BLOCKED_PKG = "blocked_pkg" // backward compat
    private const val EXTRA_Z = "z_score"
    private const val EXTRA_REASON = "reason"

    private var currentSessionStartElapsedMs: Long? = null;

    fun onAccessibilityConnected(context: Context) {
        Log.d(TAG, "Accessibility service connected")
    }

    /**
     * Called whenever foreground package changes.
     */
    fun onForegroundPackageChanged(context: Context, packageName: String) {
        // Ignore our own app to prevent loops when BlockerActivity is showing
        if (packageName.startsWith(context.packageName)) return

        // throttle noisy events
        val now = SystemClock.elapsedRealtime()
        if (packageName == lastHandledPkg && (now - lastHandledAtMs) < MIN_HANDLE_GAP_MS) return
        lastHandledPkg = packageName
        lastHandledAtMs = now

        scope.launch {
            try {
                val prefsManager = PreferencesManager(context)

                // 1) Read enabled tracked ids
                val enabledIds: Set<TrackedAppId> = prefsManager.enabledTrackedApps.first()

                // 2) Convert enabled IDs -> enabled package set (this is what we actually check)
                val enabledPackages: Set<String> =
                    enabledIds.flatMap { id -> TrackedApps.metaFor(id).exactPackages }.toSet()

                // ✅ IMPORTANT: tracked 판단只用 enabledPackages
                val isTracked = packageName in enabledPackages

                if (!isTracked) {
                    // leaving blocked session -> reset session flags
                    if (sessionState.inBlockedSession) {
                        Log.d(TAG, "Leaving tracked session, reset state.")
                    }
                    currentSessionStartElapsedMs = null
                    sessionState = sessionState.copy(
                        inBlockedSession = false,
                        currentPackage = null,
                        askedUsageTypeThisSession = false
                    )
                    return@launch
                }

                // entering tracked session
                if (!sessionState.inBlockedSession || sessionState.currentPackage != packageName) {
                    Log.d(TAG, "Entering tracked session: $packageName")
                    currentSessionStartElapsedMs = now
                    sessionState = sessionState.copy(
                        inBlockedSession = true,
                        currentPackage = packageName,
                        askedUsageTypeThisSession = false
                    )
                }

                // Build usecase with providers
                val useCase = buildUseCase(context)
                val sessionStart = currentSessionStartElapsedMs ?: now
                val currentSessionMinutes = ((now - sessionStart) / 60_000L).toInt().coerceAtLeast(0)

                val (newState, result) = useCase.run(
                    nowMs = now,
                    state = sessionState,
                    currentSessionMinutes = currentSessionMinutes
                )
                sessionState = newState

                when (result) {
                    is TriggerResult.None -> Unit

                    is TriggerResult.AskUsageType -> {
                        launchGapGuardOrSkip {
                            showAskUsageType(
                                context = context,
                                targetPkg = packageName,
                                zScore = 0.0 // 如果要显示z：让 UseCase 把 z 带出来即可
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

    // =========================
    // UseCase wiring (Providers)
    // =========================

    private fun buildUseCase(context: Context): MaybeRunInterventionCheckUseCase {
        val prefsManager = PreferencesManager(context)
        val usageRepoReal = UsageStatsRepository(context)
        val localUsageRepo: LocalUsageRepo = usageRepoReal

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
            localUsageRepo = localUsageRepo
        )
    }

    // =========================
    // Helpers
    // =========================

    private inline fun launchGapGuardOrSkip(block: () -> Unit) {
        val launchNow = SystemClock.elapsedRealtime()
        if ((launchNow - lastLaunchedAtMs) < MIN_LAUNCH_GAP_MS) {
            Log.d(TAG, "Launch throttled (gap). Skip showing UI.")
            return
        }
        lastLaunchedAtMs = launchNow
        block()
    }

    // =========================
    // UI Launchers
    // =========================

    private fun showAskUsageType(context: Context, targetPkg: String, zScore: Double) {
        Log.d(TAG, "Show ASK_USAGE_TYPE for $targetPkg")

        val i = Intent(context, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_ASK_USAGE_TYPE)
            putExtra(EXTRA_TARGET_PKG, targetPkg)
            putExtra(EXTRA_BLOCKED_PKG, targetPkg) // backward compat
            putExtra(EXTRA_Z, zScore)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(i)
    }

    private fun showInterventionPlaceholder(context: Context, targetPkg: String, zScore: Double) {
        Log.d(TAG, "Show MODE_BLOCK placeholder for $targetPkg z=$zScore")

        val reason = "z=${String.format("%.2f", zScore)}"

        val i = Intent(context, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_BLOCK)
            putExtra(EXTRA_TARGET_PKG, targetPkg)
            putExtra(EXTRA_BLOCKED_PKG, targetPkg)
            putExtra(EXTRA_Z, zScore)
            putExtra(EXTRA_REASON, reason)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(i)
    }
}