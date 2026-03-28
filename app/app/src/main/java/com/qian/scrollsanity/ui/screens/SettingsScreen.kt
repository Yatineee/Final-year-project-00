package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.local.permissions.AccessibilityPermissionDataSource
import com.qian.scrollsanity.data.local.permissions.UsageAccessPermissionDataSource
import com.qian.scrollsanity.domain.util.TrackedApps
import com.qian.scrollsanity.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenGoals: () -> Unit,
    onOpenInterests: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    val usagePermissionDataSource = remember { UsageAccessPermissionDataSource(context) }
    val accessibilityPermissionDataSource = remember { AccessibilityPermissionDataSource(context) }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val intensity by viewModel.interventionIntensity.collectAsStateWithLifecycle()
    val tone by viewModel.toneStyle.collectAsStateWithLifecycle()
    val interests by viewModel.interests.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val enabledTracked by viewModel.enabledTrackedApps.collectAsStateWithLifecycle()

    var hasUsagePermission by remember {
        mutableStateOf(usagePermissionDataSource.hasPermission())
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(accessibilityPermissionDataSource.hasPermission())
    }

    PermissionRefreshOnResume {
        hasUsagePermission = usagePermissionDataSource.hasPermission()
        hasAccessibilityPermission = accessibilityPermissionDataSource.hasPermission()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile?.nickname?.firstOrNull()?.uppercase() ?: "A",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column {
                            Text(
                                text = profile?.displayName ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = profile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    var nicknameInput by remember(profile?.nickname) {
                        mutableStateOf(profile?.nickname ?: "")
                    }

                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { nicknameInput = it },
                        label = { Text("Nickname (GPT calls you)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.updateNickname(nicknameInput) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Nickname")
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            FirebaseAuth.getInstance().signOut()
                            onLogout()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        item { SectionHeader("Intervention") }

        item {
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text("Intensity", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { value ->
                            FilterChip(
                                selected = intensity == value,
                                onClick = { viewModel.updateInterventionIntensity(value) },
                                label = {
                                    Text(
                                        value.lowercase().replaceFirstChar { it.uppercase() }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Tone", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("gentle", "direct", "humorous", "coach").forEach { style ->
                            FilterChip(
                                selected = tone == style,
                                onClick = { viewModel.updateToneStyle(style) },
                                label = {
                                    Text(style.replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text("Notifications", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateNotificationsEnabled(enabled)
                        }
                    )
                }
            }
        }

        item {
            SectionHeader("Goals")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.Timer,
                    title = "Manage Goals",
                    subtitle = "View and edit your goals",
                    onClick = onOpenGoals
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            val interestCount = interests.size
            val preview = interests.take(3)

            SectionHeader("Interests")
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.TrackChanges,
                    title = "Interests",
                    subtitle = "$interestCount saved • Tap to manage",
                    onClick = onOpenInterests
                )

                if (preview.isNotEmpty()) {
                    Column(
                        modifier = Modifier.padding(
                            start = 56.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        preview.forEach { interest ->
                            Text(
                                text = "• ${interest.text}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        if (interestCount > 3) {
                            Text(
                                text = "+${interestCount - 3} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        item { SectionHeader("Tracked Apps") }

        item {
            SettingsCard {
                TrackedApps.all.forEachIndexed { index, meta ->
                    SettingsToggleRow(
                        icon = Icons.Rounded.TrackChanges,
                        title = meta.displayName,
                        checked = meta.id in enabledTracked,
                        onCheckedChange = { isOn ->
                            viewModel.setTrackedAppEnabled(meta.id, isOn)
                        }
                    )

                    if (index != TrackedApps.all.lastIndex) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item { SectionHeader("Permissions") }

        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.BarChart,
                    title = "Usage Access",
                    subtitle = if (hasUsagePermission) "Granted ✓" else "Required for tracking",
                    onClick = {
                        if (!hasUsagePermission) {
                            usagePermissionDataSource.openSettings()
                        }
                    }
                )

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Lock,
                    title = "Accessibility Service",
                    subtitle = if (hasAccessibilityPermission) {
                        "Enabled ✓"
                    } else {
                        "Required for blocking apps"
                    },
                    onClick = {
                        if (!hasAccessibilityPermission) {
                            accessibilityPermissionDataSource.openSettings()
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        item { SectionHeader("About") }

        item {
            SettingsCard {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    showArrow = false,
                    onClick = {}
                )

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Policy,
                    title = "Privacy Policy",
                    onClick = {}
                )
            }
            Spacer(Modifier.height(32.dp))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ScrollSanity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Master your attention.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column { content() }
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PermissionRefreshOnResume(onResume: () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) onResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}