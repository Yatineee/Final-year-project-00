package com.qian.scrollsanity.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.PreferencesManager
import com.qian.scrollsanity.data.TrackedApps
import com.qian.scrollsanity.data.usagedata.UsageStatsRepository
import com.qian.scrollsanity.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenGoals: () -> Unit,
    onOpenInterests: () -> Unit,
    onLogout: () -> Unit
) {

    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }
    val usageRepo = remember { UsageStatsRepository(context) }
    val scope = rememberCoroutineScope()

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val intensity by viewModel.interventionIntensity.collectAsStateWithLifecycle()
    val tone by viewModel.toneStyle.collectAsStateWithLifecycle()
    val interests by viewModel.interests.collectAsStateWithLifecycle()
    // val recentGoal by viewModel.recentGoalContext.collectAsStateWithLifecycle()

    val notificationsEnabled by prefsManager.notificationsEnabled.collectAsState(initial = true)
    val enabledTracked by prefsManager.enabledTrackedApps.collectAsState(initial = TrackedApps.allIds)

    var hasPermission by remember { mutableStateOf(usageRepo.hasUsagePermission()) }
    var hasAccessibility by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    PermissionRefreshOnResume {
        hasPermission = usageRepo.hasUsagePermission()
        hasAccessibility = isAccessibilityServiceEnabled(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // =========================
        // PROFILE
        // =========================

        item {
            SettingsCard {
                Column(Modifier.padding(16.dp)) {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        Surface(
                            modifier = Modifier.size(56.dp).clip(CircleShape),
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
                        Icon(Icons.AutoMirrored.Rounded.Logout, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // =========================
        // =========================
        // INTERVENTION
        // =========================

        item { SectionHeader("Intervention") }

        item {
            SettingsCard {
                Column(Modifier.padding(16.dp)) {

                    Text("Intensity", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach {
                            FilterChip(
                                selected = intensity == it,
                                onClick = { viewModel.updateInterventionIntensity(it) },
                                label = { Text(it.lowercase().replaceFirstChar { c -> c.uppercase() }) }
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
                                label = { Text(style.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

//                    var goalInput by remember(recentGoal) {
//                        mutableStateOf(recentGoal ?: "")
//                    }

//                    Text("Current Focus Goal", fontWeight = FontWeight.SemiBold)
//                    Spacer(Modifier.height(8.dp))
//
//                    OutlinedTextField(
//                        value = goalInput,
//                        onValueChange = { goalInput = it },
//                        modifier = Modifier.fillMaxWidth(),
//                        label = { Text("e.g. Prepare IELTS") }
//                    )
//
//                    Spacer(Modifier.height(8.dp))
//
//                    Button(
//                        onClick = { viewModel.updateRecentGoalContext(goalInput) },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Save Goal Context")
//                    }

//                    Spacer(Modifier.height(20.dp))

//                    var interestsInput by remember(interests) {
//                        mutableStateOf(interests.joinToString(", "))
//                    }
//
//                    Text("Interests", fontWeight = FontWeight.SemiBold)
//                    Spacer(Modifier.height(8.dp))
//
//                    OutlinedTextField(
//                        value = interestsInput,
//                        onValueChange = { interestsInput = it },
//                        modifier = Modifier.fillMaxWidth(),
//                        label = { Text("Comma separated") }
//                    )
//
//                    Spacer(Modifier.height(8.dp))

//                    Button(
//                        onClick = {
//                            viewModel.updateInterests(
//                                interestsInput.split(",").map { it.trim() }
//                            )
//                        },
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Text("Save Interests")
//                    }
                }
            }

//            Spacer(Modifier.height(16.dp))
        }

        // =========================
        // GOALS
        // =========================

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

        // =========================
        // INTERESTS
        // =========================

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
                    Column(Modifier.padding(start = 56.dp, end = 16.dp, bottom = 16.dp)) {
                        preview.forEach { i ->
                            Text(
                                text = "• $i",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(16.dp))
        }

        // =========================
        // TRACKED APPS
        // =========================

        item { SectionHeader("Tracked Apps") }

        item {
            SettingsCard {
                TrackedApps.all.forEachIndexed { index, meta ->
                    SettingsToggleRow(
                        icon = Icons.Rounded.TrackChanges,
                        title = meta.displayName,
                        checked = meta.id in enabledTracked,
                        onCheckedChange = { isOn ->
                            scope.launch { prefsManager.setTrackedAppEnabled(meta.id, isOn) }
                        }
                    )

                    if (index != TrackedApps.all.lastIndex) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }



        // =========================
        // PERMISSIONS
        // =========================

        item { SectionHeader("Permissions") }

        item {
            SettingsCard {

                SettingsRow(
                    icon = Icons.Rounded.BarChart,
                    title = "Usage Access",
                    subtitle = if (hasPermission) "Granted ✓" else "Required for tracking",
                    onClick = {
                        if (!hasPermission) usageRepo.openUsageAccessSettings()
                    }
                )

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    icon = Icons.Rounded.Lock,
                    title = "Accessibility Service",
                    subtitle = if (hasAccessibility) "Enabled ✓" else "Required for blocking apps",
                    onClick = {
                        if (!hasAccessibility) {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // =========================
        // ABOUT
        // =========================

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

        // =========================
        // FOOTER
        // =========================

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

// Small helper chip to keep style consistent
//@Composable
//private fun IntensityChip(
//    label: String,
//    selected: Boolean,
//    enabled: Boolean,
//    onClick: () -> Unit
//) {
//    FilterChip(
//        selected = selected,
//        enabled = enabled,
//        onClick = onClick,
//        label = { Text(label) }
//    )
//}

//private fun setIntensity(
//    uid: String?,
//    chosen: String,
//    setLocal: (String) -> Unit,
//    setSaving: (Boolean) -> Unit,
//    setError: (String?) -> Unit,
//    repo: FirestoreRepository,
//    scope: kotlinx.coroutines.CoroutineScope
//) {
//    setError(null)
//    setLocal(chosen)
//
//    if (uid == null) {
//        setError("Not signed in.")
//        return
//    }
//
//    setSaving(true)
//    scope.launch {
//        val res = repo.updateInterventionIntensity(uid, chosen)
//        setSaving(false)
//        if (res.isFailure) {
//            setError(res.exceptionOrNull()?.message ?: "Failed to save intensity")
//        }
//    }
//}

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

/**
 * Refresh callback when returning to the app/screen.
 * Keeps Settings permission subtitles accurate after user grants permission.
 */
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

/**
 * Check if the Accessibility Service is enabled for app blocking
 * ⚠️ 这里的 serviceName 必须和你 Manifest 里声明的 service 完全一致。
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val packageName = context.packageName
    val serviceName = "$packageName/.blocker.AccessibilityMonitorService"

    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    return enabledServices.contains(serviceName)
}