package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.qian.scrollsanity.domain.model.dashboard.DailyAverageSessionPoint
import com.qian.scrollsanity.ui.dashboard.DashboardViewModel

@Composable
fun FocusScreen(
    viewModel: DashboardViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Focus",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = "Real-time focus risk indicators",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        MetricCard(
            title = "Current z-score",
            value = summary?.currentZScore?.let { String.format("%.2f", it) } ?: "--"
        )

        MetricCard(
            title = "Next intervention threshold",
            value = summary?.interventionThreshold?.let { String.format("%.2f", it) } ?: "--"
        )

        MetricCard(
            title = "Interventions today",
            value = summary?.interventionsToday?.toString() ?: "0"
        )

        DailyAverageSessionChart(
            points = summary?.dailyAverageSessionPoints.orEmpty()
        )
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun DailyAverageSessionChart(
    points: List<DailyAverageSessionPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val todayAverage = points.lastOrNull()?.averageMinutes ?: 0.0
    val peakAverage = points.maxOfOrNull { it.averageMinutes } ?: 0.0

    Text(
        text = "Today: ${"%.1f".format(todayAverage)} min · Peak: ${"%.1f".format(peakAverage)} min",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Daily average session length",
                style = MaterialTheme.typography.titleMedium
            )

            if (points.isEmpty()) {
                Text(
                    text = "No session data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            val maxValue = (points.maxOfOrNull { it.averageMinutes } ?: 0.0).coerceAtLeast(1.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (points.size < 2) return@Canvas

                val width = size.width
                val height = size.height
                val stepX = width / (points.size - 1)

                val offsets = points.mapIndexed { index, point ->
                    val x = stepX * index
                    val y = height - ((point.averageMinutes / maxValue) * height).toFloat()
                    Offset(x, y)
                }

                for (i in 0 until offsets.lastIndex) {
                    drawLine(
                        color = lineColor,
                        start = offsets[i],
                        end = offsets[i + 1],
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                }

                offsets.forEach { offset ->
                    drawCircle(
                        color = lineColor,
                        radius = 7f,
                        center = offset
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                points.forEach { point ->
                    Text(
                        text = point.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}