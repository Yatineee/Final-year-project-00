package com.qian.scrollsanity.controller.blocker.old.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.qian.scrollsanity.MainActivity
import com.qian.scrollsanity.controller.blocker.old.bus.InterventionEventBus
import com.qian.scrollsanity.ui.theme.AreteTheme

class BlockerActivity : ComponentActivity() {

    companion object {
        // must match BlockerController
        const val EXTRA_MODE = "mode"
        const val MODE_ASK_USAGE_TYPE = "ASK_USAGE_TYPE"
        const val MODE_ASK_MENTAL_STATE = "ASK_MENTAL_STATE"
        const val MODE_BLOCK = "BLOCK"

        const val EXTRA_TARGET_PKG = "target_pkg"
        const val EXTRA_BLOCKED_PKG = "blocked_pkg" // backward compat
        const val EXTRA_REASON = "reason"
        const val EXTRA_Z = "z_score"

        // Stage 1 decision values
        const val EXTRA_USAGE_TYPE = "usage_type"
        const val USAGE_INTENTIONAL = "intentional"
        const val USAGE_HABIT = "habit"

        // Stage 2 decision values
        const val EXTRA_MENTAL_STATE = "mental_state"      // "bored"|"stress"|"inertia"
        const val EXTRA_ENGAGING = "engaging"              // Boolean

        const val MENTAL_BORED = "bored"
        const val MENTAL_STRESS = "stress"
        const val MENTAL_INERTIA = "inertia"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_BLOCK

        // target package
        val targetPkg = intent.getStringExtra(EXTRA_TARGET_PKG)
            ?: intent.getStringExtra(EXTRA_BLOCKED_PKG)
            ?: ""

        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Intervention"
        val zScore = intent.getDoubleExtra(EXTRA_Z, 0.0)

        setContent {
            AreteTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when (mode) {
                        MODE_ASK_USAGE_TYPE -> {
                            AskUsageTypeScreen(
                                zScore = zScore,
                                onPick = { picked ->
                                    // ✅ SharedFlow EventBus (no broadcast)
                                    InterventionEventBus.emit(
                                        InterventionEventBus.Event.UsageTypeDecided(
                                            pkg = targetPkg,
                                            usageType = picked,
                                            z = zScore
                                        )
                                    )
                                    finish()
                                },
                                onDismiss = { finish() }
                            )
                        }

                        MODE_ASK_MENTAL_STATE -> {
                            AskMentalStateScreen(
                                zScore = zScore,
                                onSubmit = { mentalState, engaging ->
                                    // ✅ SharedFlow EventBus (no broadcast)
                                    InterventionEventBus.emit(
                                        InterventionEventBus.Event.MentalStateDecided(
                                            pkg = targetPkg,
                                            mentalState = mentalState,
                                            engaging = engaging,
                                            z = zScore
                                        )
                                    )
                                    finish()
                                },
                                onDismiss = { finish() }
                            )
                        }

                        else -> {
                            // Default: old blocker UI
                            InterventionScreen(
                                targetPackage = targetPkg,
                                reason = reason,
                                onContinue = { finish() },
                                onExit = {
                                    startActivity(Intent(this, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    })
                                    finish()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AskUsageTypeScreen(
    zScore: Double,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Quick check", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Are you using this app intentionally, or just out of habit?",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (zScore != 0.0) {
                    Text(
                        "z = ${"%.2f".format(zScore)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(BlockerActivity.USAGE_INTENTIONAL) }
                    ) { Text("Intentional") }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onPick(BlockerActivity.USAGE_HABIT) }
                    ) { Text("Habit") }
                }

                TextButton(
                    modifier = Modifier.align(Alignment.End),
                    onClick = onDismiss
                ) { Text("Not now") }
            }
        }
    }
}

@Composable
private fun AskMentalStateScreen(
    zScore: Double,
    onSubmit: (mentalState: String, engaging: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedState by remember { mutableStateOf<String?>(null) }
    var engaging by remember { mutableStateOf<Boolean?>(null) }

    val canSubmit = selectedState != null && engaging != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("One more thing", style = MaterialTheme.typography.titleLarge)

                Text(
                    "What’s driving the scroll right now?",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (zScore != 0.0) {
                    Text(
                        "z = ${"%.2f".format(zScore)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Q1: mental state
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceRow(
                        label = "Bored",
                        selected = selectedState == BlockerActivity.MENTAL_BORED,
                        onClick = { selectedState = BlockerActivity.MENTAL_BORED }
                    )
                    ChoiceRow(
                        label = "Stressed",
                        selected = selectedState == BlockerActivity.MENTAL_STRESS,
                        onClick = { selectedState = BlockerActivity.MENTAL_STRESS }
                    )
                    ChoiceRow(
                        label = "Inertia (can’t stop / autopilot)",
                        selected = selectedState == BlockerActivity.MENTAL_INERTIA,
                        onClick = { selectedState = BlockerActivity.MENTAL_INERTIA }
                    )
                }

                HorizontalDivider()

                // Q2: engaging
                Text(
                    "Are you currently engaged in a meaningful activity?",
                    style = MaterialTheme.typography.bodyMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = engaging == true,
                        onClick = { engaging = true },
                        label = { Text("Yes") }
                    )
                    FilterChip(
                        selected = engaging == false,
                        onClick = { engaging = false },
                        label = { Text("No") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    ) { Text("Back") }

                    Button(
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        onClick = { onSubmit(selectedState!!, engaging!!) }
                    ) { Text("Continue") }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (selected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}