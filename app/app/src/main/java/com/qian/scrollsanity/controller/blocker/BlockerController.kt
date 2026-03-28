package com.qian.scrollsanity.controller.blocker

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import com.qian.scrollsanity.blocker.controller.BlockerRuntimeFacade
import com.qian.scrollsanity.controller.blocker.old.ui.BlockerActivity
import com.qian.scrollsanity.domain.tempdata.LiveSessionStateHolder
import com.qian.scrollsanity.domain.trigger.InterventionSessionState
import com.qian.scrollsanity.domain.usecase.old.intervention.TriggerResult
import com.qian.scrollsanity.domain.util.TrackedApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

object BlockerController {

    private const val TAG = "BlockerController"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var appContext: Context? = null

    // =========================
    // Event throttles
    // =========================

    private var lastHandledPkg: String? = null
    private var lastHandledAtMs: Long = 0L
    private const val MIN_HANDLE_GAP_MS = 400L

    private var lastLaunchedAtMs: Long = 0L
    private const val MIN_LAUNCH_GAP_MS = 1200L

    // =========================
    // Live ticker
    // =========================

    private var minuteTickerStarted = false

    // =========================
    // Intervention session state
    // =========================

    private var sessionState = InterventionSessionState()

    // =========================
    // BlockerActivity extras
    // =========================

    private const val EXTRA_MODE = "mode"
    private const val MODE_ASK_USAGE_TYPE = "ASK_USAGE_TYPE"
    private const val MODE_BLOCK = "BLOCK"

    private const val EXTRA_TARGET_PKG = "target_pkg"
    private const val EXTRA_BLOCKED_PKG = "blocked_pkg"
    private const val EXTRA_Z = "z_score"
    private const val EXTRA_REASON = "reason"

    fun onAccessibilityConnected(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "Accessibility service connected")
        startMinuteTickerIfNeeded()
    }

    private fun startMinuteTickerIfNeeded() {
        if (minuteTickerStarted) return
        minuteTickerStarted = true

        scope.launch {
            while (true) {
                delay(1000)

                val now = SystemClock.elapsedRealtime()
                LiveSessionStateHolder.updateMinutes(now)

                val context = appContext ?: continue
                val liveState = LiveSessionStateHolder.state.value

                if (!liveState.inTrackedSession) continue
                if (!sessionState.inBlockedSession) continue

                try {
                    val facade = BlockerRuntimeFacade(context)

                    val oldState = sessionState
                    val currentMinutes = liveState.currentSessionMinutes

                    val (newState, result) = facade.runInterventionCheck(
                        nowMs = now,
                        state = sessionState,
                        currentSessionMinutes = currentMinutes
                    )

                    sessionState = newState

                    Log.d(
                        TAG,
                        "Ticker intervention evaluated: oldState=$oldState, newState=$newState, result=$result, currentMinutes=$currentMinutes, package=${liveState.packageName}"
                    )

                    when (result) {
                        is TriggerResult.None -> Unit

                        is TriggerResult.AskUsageType -> {
                            val targetPkg = liveState.packageName ?: continue
                            launchGapGuardOrSkip {
                                showAskUsageType(
                                    context = context,
                                    targetPkg = targetPkg,
                                    zScore = 0.0
                                )
                            }
                        }

                        is TriggerResult.TriggerIntervention -> {
                            val targetPkg = liveState.packageName ?: continue
                            facade.recordNextThresholdAfterTrigger(result.z)

                            launchGapGuardOrSkip {
                                showInterventionPlaceholder(
                                    context = context,
                                    targetPkg = targetPkg,
                                    zScore = result.z
                                )
                            }
                        }
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Error in minute ticker intervention loop", t)
                }
            }
        }
    }

    fun onForegroundPackageChanged(context: Context, packageName: String) {
        appContext = context.applicationContext

        if (packageName.startsWith(context.packageName)) return

        val now = SystemClock.elapsedRealtime()

        if (packageName == lastHandledPkg && (now - lastHandledAtMs) < MIN_HANDLE_GAP_MS) {
            return
        }
        lastHandledPkg = packageName
        lastHandledAtMs = now

        scope.launch {
            try {
                val facade = BlockerRuntimeFacade(context)
                val enabledIds = facade.getEnabledTrackedIds()
                val enabledPackages = enabledIds
                    .flatMap { id -> TrackedApps.metaFor(id).exactPackages }
                    .toSet()

                val isTracked = packageName in enabledPackages

                Log.d(
                    TAG,
                    "foreground=$packageName isTracked=$isTracked sessionState=$sessionState"
                )

                if (!isTracked) {
                    if (sessionState.inBlockedSession) {
                        Log.d(TAG, "Leaving tracked session, reset state")
                        LiveSessionStateHolder.endSession()
                    }

                    sessionState = sessionState.copy(
                        inBlockedSession = false,
                        currentPackage = null,
                        lastCheckAtMs = 0L,
                        askedUsageTypeThisSession = false
                    )
                    return@launch
                }

                if (!sessionState.inBlockedSession || sessionState.currentPackage != packageName) {
                    Log.d(TAG, "Entering tracked session: $packageName")

                    sessionState = sessionState.copy(
                        inBlockedSession = true,
                        currentPackage = packageName,
                        lastCheckAtMs = 0L,
                        askedUsageTypeThisSession = false
                    )

                    val trackedAppId = enabledIds.firstOrNull { id ->
                        packageName in TrackedApps.metaFor(id).exactPackages
                    }

                    if (trackedAppId != null) {
                        LiveSessionStateHolder.startSession(
                            appId = trackedAppId,
                            packageName = packageName,
                            startElapsedMs = now
                        )
                    } else {
                        Log.w(TAG, "Tracked package matched but no trackedAppId found for $packageName")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Error in onForegroundPackageChanged($packageName)", t)
            }
        }
    }

    private inline fun launchGapGuardOrSkip(block: () -> Unit) {
        val launchNow = SystemClock.elapsedRealtime()
        if ((launchNow - lastLaunchedAtMs) < MIN_LAUNCH_GAP_MS) {
            Log.d(TAG, "Launch throttled (gap), skip showing UI")
            return
        }
        lastLaunchedAtMs = launchNow
        block()
    }

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

    private fun showInterventionPlaceholder(context: Context, targetPkg: String, zScore: Double) {
        Log.d(TAG, "Show MODE_BLOCK placeholder for $targetPkg, z=$zScore")

        val reason = "z=${String.Companion.format(Locale.US, "%.2f", zScore)}"

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