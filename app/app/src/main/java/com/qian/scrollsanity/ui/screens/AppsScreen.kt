package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qian.scrollsanity.data.AppCategory
import com.qian.scrollsanity.data.AppUsageInfo
import com.qian.scrollsanity.data.UsageStatsRepository
import com.qian.scrollsanity.ui.util.formatTime


/**
 * AppsScreen changes (focused MVP):
 * - Remove category chips (you only track the 7 social apps for now).
 * - Show a single "Tracked Apps" list (all 7, even if 0m).
 * - Summary card becomes "Tracked Screen Time" + "Used today" count.
 * - If permission missing: show the same permission gate messaging (simple).
 *
 * Assumption:
 * - UsageStatsRepository.getTodayUsageStats() now returns ONLY the tracked apps (7),
 *   including 0-minute entries.
 */
@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val usageRepo = remember { UsageStatsRepository(context) }

    var todayStats by remember { mutableStateOf<List<AppUsageInfo>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(usageRepo.hasUsagePermission()) }

    LaunchedEffect(Unit) {
        hasPermission = usageRepo.hasUsagePermission()
        if (hasPermission) {
            todayStats = usageRepo.getTodayUsageStats()
        }
    }

    val totalMinutes = todayStats.sumOf { it.usageTimeMinutes }
    val usedTodayCount = todayStats.count { it.usageTimeMinutes > 0 }
    val maxMinutes = todayStats.maxOfOrNull { it.usageTimeMinutes } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tracked Apps",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tracked Screen Time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(totalMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$usedTodayCount of ${todayStats.size} used today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Apps List
        if (!hasPermission) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Usage permission required",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (todayStats.isEmpty()) {
            // Defensive: with your updated repository you should still get 7 entries.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tracked app data yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todayStats) { app ->
                    AppDetailRow(
                        app = app,
                        rank = todayStats.indexOf(app) + 1,
                        maxMinutes = maxMinutes
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun AppDetailRow(
    app: AppUsageInfo,
    rank: Int,
    maxMinutes: Int
) {
    val progress = if (maxMinutes <= 0) 0f else app.usageTimeMinutes.toFloat() / maxMinutes.toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(32.dp)
            )

            // App icon placeholder (all are SOCIAL for MVP)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(getCategoryColor(AppCategory.SOCIAL).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = app.appName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = getCategoryColor(AppCategory.SOCIAL)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatTime(app.usageTimeMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = getCategoryColor(AppCategory.SOCIAL),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun getCategoryColor(category: AppCategory): Color {
    return when (category) {
        AppCategory.SOCIAL -> Color(0xFFE1306C)
        AppCategory.ENTERTAINMENT -> Color(0xFFFF0000)
        AppCategory.PRODUCTIVITY -> Color(0xFF4285F4)
        AppCategory.GAMES -> Color(0xFF9C27B0)
        AppCategory.OTHER -> MaterialTheme.colorScheme.primary
    }
}
