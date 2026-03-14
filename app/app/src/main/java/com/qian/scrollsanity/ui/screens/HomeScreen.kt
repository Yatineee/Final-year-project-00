package com.qian.scrollsanity.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qian.scrollsanity.data.usagedata.AppUsageInfo
import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.data.usagedata.TrackedApps
import com.qian.scrollsanity.data.usagedata.UsageStatsRepository
import com.qian.scrollsanity.ui.theme.AreteAccent
import com.qian.scrollsanity.ui.theme.AreteDanger
import com.qian.scrollsanity.ui.theme.AretePrimary
import com.qian.scrollsanity.ui.theme.AreteWarning
import com.qian.scrollsanity.ui.util.formatTime

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val usageRepo = remember { UsageStatsRepository(context) }
    val prefsManager = remember { PreferencesManager(context) }

    var hasUsagePermission by remember { mutableStateOf(usageRepo.hasUsagePermission()) }
    var hasAccessibilityPermission by remember { mutableStateOf(usageRepo.hasAccessibilityPermission()) }
    var todayStats by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }

    val dailyGoal by prefsManager.dailyGoalMinutes.collectAsState(initial = 240)

    // Per-app toggles (enabled set of TrackedAppId)
    val enabledTracked by prefsManager.enabledTrackedApps.collectAsState(initial = TrackedApps.allIds)

    // Auto-refresh permissions when screen resumes
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = usageRepo.hasUsagePermission()
                hasAccessibilityPermission = usageRepo.hasAccessibilityPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Refresh data whenever permission or toggles change
    LaunchedEffect(hasUsagePermission, enabledTracked) {
        if (hasUsagePermission) {
            todayStats = usageRepo.getTodayUsageStats(enabledTracked)
        } else {
            todayStats = emptyList()
        }
    }

    val totalMinutes = todayStats.sumOf { it.usageTimeMinutes }
    val progressPercent = (totalMinutes.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1.5f)

    val usedTodayCount = todayStats.count { it.usageTimeMinutes > 0 }

    if (!hasUsagePermission || !hasAccessibilityPermission) {
        PermissionRequest(
            hasUsagePermission = hasUsagePermission,
            hasAccessibilityPermission = hasAccessibilityPermission,
            onGrantUsageClick = {
                usageRepo.openUsageAccessSettings()
            },
            onGrantAccessibilityClick = {
                usageRepo.openAccessibilitySettings()
            },
            onRefreshClick = {
                hasUsagePermission = usageRepo.hasUsagePermission()
                hasAccessibilityPermission = usageRepo.hasAccessibilityPermission()
            }
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Progress Ring
            item {
                ProgressRingCard(
                    totalMinutes = totalMinutes,
                    goalMinutes = dailyGoal,
                    progress = progressPercent
                )
            }

            // Quick Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.TouchApp,
                        value = "$usedTodayCount",
                        label = "Used Today"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Timer,
                        value = formatTime(dailyGoal - totalMinutes),
                        label = if (totalMinutes <= dailyGoal) "Remaining" else "Over Goal"
                    )
                }
            }

            // Tracked Apps (enabled only)
            item {
                Text(
                    text = "Tracked Apps",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (todayStats.isNotEmpty()) {
                val maxMinutes = todayStats.maxOfOrNull { it.usageTimeMinutes } ?: 0
                items(todayStats) { app ->
                    AppUsageRow(
                        app = app,
                        maxMinutes = maxMinutes
                    )
                }
            } else {
                item {
                    Text(
                        text = "No tracked apps enabled.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Motivation Card
            item {
                MotivationCard(progressPercent)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun PermissionRequest(
    hasUsagePermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onGrantUsageClick: () -> Unit,
    onGrantAccessibilityClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚙️",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Arete needs these permissions to track and limit your app usage. Your data never leaves your device.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Usage Permission
        if (!hasUsagePermission) {
            PermissionItem(
                icon = "📊",
                title = "Usage Access",
                description = "Required to track your app usage time",
                isGranted = false,
                onGrantClick = onGrantUsageClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accessibility Permission
        if (!hasAccessibilityPermission) {
            PermissionItem(
                icon = "🔒",
                title = "Accessibility Service",
                description = "Required to block apps when limit is reached",
                isGranted = false,
                onGrantClick = onGrantAccessibilityClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRefreshClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("I've Granted Them - Refresh")
        }
    }
}

@Composable
fun PermissionItem(
    icon: String,
    title: String,
    description: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (!isGranted) {
                Button(onClick = onGrantClick) {
                    Text("Grant")
                }
            }
        }
    }
}

@Composable
fun ProgressRingCard(
    totalMinutes: Int,
    goalMinutes: Int,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    val progressColor = when {
        progress >= 1f -> AreteDanger
        progress >= 0.8f -> AreteWarning
        else -> AretePrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Background ring
                Canvas(modifier = Modifier.size(180.dp)) {
                    drawArc(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Progress ring
                Canvas(modifier = Modifier.size(180.dp)) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatTime(totalMinutes),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${formatTime(goalMinutes)} goal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AppUsageRow(
    app: AppUsageInfo,
    maxMinutes: Int
) {
    val context = LocalContext.current
    val progress = if (maxMinutes <= 0) 0f else app.usageTimeMinutes.toFloat() / maxMinutes.toFloat()

    // Get app icon from package manager
    val appIcon = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App icon
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                // Fallback to first letter if icon can't be loaded
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = formatTime(app.usageTimeMinutes),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MotivationCard(progress: Float) {
    val (emoji, message) = when {
        progress < 0.5f -> "💪" to "Great start! Keep being intentional with your time."
        progress < 0.8f -> "⏰" to "You're making progress. Stay mindful of your usage."
        progress < 1f -> "⚠️" to "Approaching your goal. Consider taking a break."
        else -> "🛑" to "You've exceeded your goal. Time to disconnect!"
    }

    val backgroundColor = when {
        progress >= 1f -> AreteDanger.copy(alpha = 0.1f)
        progress >= 0.8f -> AreteWarning.copy(alpha = 0.1f)
        else -> AreteAccent.copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
