package com.qian.scrollsanity.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.ui.theme.AretePrimaryDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FocusPreset(
    val name: String,
    val emoji: String,
    val durationMinutes: Int,
    val description: String
)

val focusPresets = listOf(
    FocusPreset("Quick Focus", "⚡", 15, "Short burst of focus"),
    FocusPreset("Pomodoro", "🍅", 25, "Classic technique"),
    FocusPreset("Deep Work", "🧠", 45, "Extended focus session"),
    FocusPreset("Flow State", "🌊", 90, "Maximum productivity")
)

@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()
    val strictMode by prefsManager.focusStrictMode.collectAsState(initial = false)
    val isFocusActive by prefsManager.isFocusActive.collectAsState(initial = false)
    val focusEndTime by prefsManager.focusEndTime.collectAsState(initial = 0L)

    var isActive by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<FocusPreset?>(null) }
    var remainingSeconds by remember { mutableStateOf(0) }
    var totalSeconds by remember { mutableStateOf(0) }

    // Restore active focus session on screen load
    LaunchedEffect(isFocusActive, focusEndTime) {
        if (isFocusActive && focusEndTime > 0) {
            val remaining = ((focusEndTime - System.currentTimeMillis()) / 1000).toInt()
            if (remaining > 0) {
                isActive = true
                remainingSeconds = remaining
                totalSeconds = remaining
                // Try to find matching preset
                selectedPreset = focusPresets.firstOrNull {
                    Math.abs(it.durationMinutes * 60 - remaining) < 120
                } ?: focusPresets[1] // Default to Pomodoro if no match
            } else {
                // Session expired, clean it up
                prefsManager.endFocusSession()
            }
        }
    }

    // Timer logic
    LaunchedEffect(isActive, remainingSeconds) {
        if (isActive && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds -= 1
        } else if (isActive && remainingSeconds == 0) {
            // Timer complete - end focus session
            isActive = false
            selectedPreset = null
            scope.launch {
                prefsManager.endFocusSession()
            }
        }
    }

    if (isActive && selectedPreset != null) {
        // Active Focus Session
        ActiveFocusView(
            preset = selectedPreset!!,
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            strictMode = strictMode,
            onEndSession = {
                isActive = false
                selectedPreset = null
                remainingSeconds = 0
                scope.launch {
                    prefsManager.endFocusSession()
                }
            }
        )
    } else {
        // Preset Selection
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Focus Mode",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Block distractions and stay present",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
            
            // Presets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(focusPresets) { preset ->
                    PresetCard(
                        preset = preset,
                        onClick = {
                            selectedPreset = preset
                            totalSeconds = preset.durationMinutes * 60
                            remainingSeconds = totalSeconds
                            isActive = true
                            // Persist focus session to enable app blocking
                            scope.launch {
                                prefsManager.startFocusSession(preset.durationMinutes)
                            }
                        }
                    )
                }
            }
            
            // Tips Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "💡 Focus Tips",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Put your phone face-down\n• Close unnecessary tabs\n• Take a break after each session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PresetCard(
    preset: FocusPreset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = preset.emoji,
                fontSize = 40.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = preset.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${preset.durationMinutes} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActiveFocusView(
    preset: FocusPreset,
    remainingSeconds: Int,
    totalSeconds: Int,
    strictMode: Boolean,
    onEndSession: () -> Unit
) {
    val progress = 1f - (remainingSeconds.toFloat() / totalSeconds.toFloat())
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500),
        label = "focusProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AretePrimaryDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = preset.emoji,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = preset.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Stay focused",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Timer Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                Canvas(modifier = Modifier.size(220.dp)) {
                    // Background ring
                    drawArc(
                        color = Color.White.copy(alpha = 0.2f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Progress ring
                    drawArc(
                        color = Color.White,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatSeconds(remainingSeconds),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Light,
                        color = Color.White
                    )
                    Text(
                        text = "remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // End button
            if (!strictMode || remainingSeconds < 60) {
                OutlinedButton(
                    onClick = onEndSession,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(Color.White.copy(alpha = 0.5f))
                    )
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("End Session")
                }
            } else {
                Text(
                    text = "🔒 Strict mode enabled",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun formatSeconds(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
