package com.qian.scrollsanity.controller.blocker.old.monitor

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.qian.scrollsanity.controller.blocker.BlockerController
import com.qian.scrollsanity.controller.blocker.old.ui.BlockerActivity
import com.qian.scrollsanity.controller.blocker.old.bus.InterventionEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.random.Random

class AccessibilityMonitorService : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityMonitor"

        // Must match BlockerActivity companion
        private const val EXTRA_MODE = BlockerActivity.Companion.EXTRA_MODE
        private const val MODE_ASK_MENTAL_STATE = BlockerActivity.Companion.MODE_ASK_MENTAL_STATE
        private const val MODE_BLOCK = BlockerActivity.Companion.MODE_BLOCK

        // Optional extra for later pipeline / API
        private const val EXTRA_STRATEGY = "strategy"
    }

    // Service lifecycle scope (cancel in onDestroy)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var collectingStarted = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")

        // ✅ Start collecting SharedFlow events once
        startCollectingInterventionEventsIfNeeded()

        BlockerController.onAccessibilityConnected(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg.isBlank()) return

        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        BlockerController.onForegroundPackageChanged(applicationContext, pkg)
    }

    override fun onInterrupt() {
        // no-op
    }

    override fun onDestroy() {
        serviceScope.cancel() // ✅ stop collecting flows, prevent leaks
        super.onDestroy()
    }

    // =========================
    // Collect SharedFlow EventBus
    // =========================

    private fun startCollectingInterventionEventsIfNeeded() {
        if (collectingStarted) return
        collectingStarted = true

        serviceScope.launch {
            Log.d(TAG, "Collecting InterventionEventBus events ✅")
            try {
                InterventionEventBus.events.collect { e ->
                    when (e) {
                        is InterventionEventBus.Event.UsageTypeDecided -> {
                            handleUsageType(
                                pkg = e.pkg,
                                usageType = e.usageType,
                                z = e.z
                            )
                        }

                        is InterventionEventBus.Event.MentalStateDecided -> {
                            handleMentalState(
                                pkg = e.pkg,
                                mental = e.mentalState,
                                engaging = e.engaging,
                                z = e.z
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                // 如果 flow 被取消/异常，不要让 service 直接炸掉
                Log.e(TAG, "EventBus collection crashed", t)
            }
        }
    }

    // =========================
    // Stage 1 result: usage type
    // =========================

    private fun handleUsageType(pkg: String, usageType: String, z: Double) {
        Log.d(TAG, "Usage type decided: pkg=$pkg usageType=$usageType z=$z")

        when (usageType) {
            BlockerActivity.Companion.USAGE_INTENTIONAL -> {
                // Intentional use → stop chain
                Log.d(TAG, "Intentional use. Stop intervention chain.")
            }

            BlockerActivity.Companion.USAGE_HABIT -> {
                // Habit → stage 2
                Log.d(TAG, "Habit use. Launching ASK_MENTAL_STATE.")
                showMentalStatePrompt(pkg, z)
            }

            else -> {
                Log.d(TAG, "Unknown usageType=$usageType (ignored)")
            }
        }
    }

    private fun showMentalStatePrompt(pkg: String, z: Double) {
        val i = Intent(this, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_ASK_MENTAL_STATE)
            putExtra(BlockerActivity.Companion.EXTRA_TARGET_PKG, pkg)
            putExtra(BlockerActivity.Companion.EXTRA_Z, z)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(i)
    }

    // =========================
    // Stage 2 result: mental state + engaging
    // =========================

    private fun handleMentalState(pkg: String, mental: String, engaging: Boolean, z: Double) {
        Log.d(TAG, "Mental state decided: pkg=$pkg mental=$mental engaging=$engaging z=$z")

        val strategy = chooseStrategy(mentalState = mental, engaging = engaging)

        // For now: show placeholder intervention (MODE_BLOCK)
        // Later: call FastAPI here (with preferences + today minutes + mental + strategy ...)
        showInterventionPlaceholder(
            pkg = pkg,
            z = z,
            mental = mental,
            engaging = engaging,
            strategy = strategy
        )
    }

    private fun chooseStrategy(mentalState: String, engaging: Boolean): String {
        val m = mentalState.lowercase()

        return when {
            engaging -> {
                // engaging -> Evoking (plus Understanding)
                weightedPick(listOf("EVOKING" to 0.65, "UNDERSTANDING" to 0.35))
            }

            else -> {
                // not engaging -> Scaffolding
                when (m) {
                    "stress" -> weightedPick(
                        listOf(
                            "SCAFFOLDING" to 0.55,
                            "COMFORTING" to 0.25,
                            "UNDERSTANDING" to 0.20
                        )
                    )

                    "inertia" -> weightedPick(
                        listOf(
                            "SCAFFOLDING" to 0.70,
                            "UNDERSTANDING" to 0.30
                        )
                    )

                    else -> weightedPick(
                        listOf(
                            "SCAFFOLDING" to 0.60,
                            "UNDERSTANDING" to 0.40
                        )
                    )
                }
            }
        }
    }

    private fun weightedPick(options: List<Pair<String, Double>>): String {
        val total = options.sumOf { it.second }
        val r = Random.nextDouble() * total
        var acc = 0.0
        for ((name, w) in options) {
            acc += w
            if (r <= acc) return name
        }
        return options.first().first
    }

    private fun showInterventionPlaceholder(
        pkg: String,
        z: Double,
        mental: String,
        engaging: Boolean,
        strategy: String
    ) {
        val reason = buildString {
            append("Strategy: ").append(strategy)
            if (mental.isNotBlank()) append(" | state=").append(mental)
            append(" | engaging=").append(engaging)
            if (z != 0.0) append(" | z=").append(String.format("%.2f", z))
        }

        val i = Intent(this, BlockerActivity::class.java).apply {
            putExtra(EXTRA_MODE, MODE_BLOCK)
            putExtra(BlockerActivity.Companion.EXTRA_TARGET_PKG, pkg)
            putExtra(BlockerActivity.Companion.EXTRA_REASON, reason)

            // keep extra context for later API integration
            putExtra(BlockerActivity.Companion.EXTRA_Z, z)
            putExtra(BlockerActivity.Companion.EXTRA_MENTAL_STATE, mental)
            putExtra(BlockerActivity.Companion.EXTRA_ENGAGING, engaging)
            putExtra(EXTRA_STRATEGY, strategy)

            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(i)
    }
}