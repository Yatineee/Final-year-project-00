package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.data.onboarding.OnboardingInput

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val firestoreRepo = remember { FirestoreRepository() }
    val userId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    var nickname by remember { mutableStateOf("") }
    var habit by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var recentChallenge by remember { mutableStateOf("") }
    var preferStyle by remember { mutableStateOf("Gentle") } // Gentle / Direct / Coach

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val canSubmit = nickname.isNotBlank() && goal.isNotBlank() && !isSaving && userId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Let’s set up your Arete profile. You can change these later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        OnboardingCard(title = "How should we call you?") {
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Nickname") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )
        }

        OnboardingCard(title = "Your daily goal") {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Goal (e.g., “≤ 2 hours”, “No TikTok after 11pm”)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }

        OnboardingCard(title = "Your usage habit (optional)") {
            OutlinedTextField(
                value = habit,
                onValueChange = { habit = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("When do you usually overuse?") }
            )
        }

        OnboardingCard(title = "Recent challenge (optional)") {
            OutlinedTextField(
                value = recentChallenge,
                onValueChange = { recentChallenge = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("What’s been hard lately?") }
            )
        }

        OnboardingCard(title = "Preferred coaching style") {
            StyleChips(
                selected = preferStyle,
                onSelected = { preferStyle = it }
            )
        }

        if (errorMsg != null) {
            Text(
                text = errorMsg!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                if (userId == null) {
                    errorMsg = "Not signed in. Please login again."
                    return@Button
                }

                isSaving = true
                errorMsg = null

                // save to Firestore
                val input = OnboardingInput(
                    nickname = nickname.trim(),
                    habit = habit.trim(),
                    goal = goal.trim(),
                    recentChallenge = recentChallenge.trim(),
                    preferStyle = preferStyle.lowercase() // "gentle" / "direct" / "coach"
                )

                // fire-and-forget in LaunchedEffect scope
                // (we use rememberCoroutineScope)
            },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(if (isSaving) "Saving..." else "Finish")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Submit logic (kept outside Button to keep composable clean)
    val scope = rememberCoroutineScope()
    LaunchedEffect(isSaving) {
        if (!isSaving) return@LaunchedEffect
        if (userId == null) return@LaunchedEffect

        val input = OnboardingInput(
            nickname = nickname.trim(),
            habit = habit.trim(),
            goal = goal.trim(),
            recentChallenge = recentChallenge.trim(),
            preferStyle = preferStyle.lowercase()
        )

        val res = firestoreRepo.completeOnboarding(userId, input)
        isSaving = false
        if (res.isSuccess) {
            onFinished()
        } else {
            errorMsg = res.exceptionOrNull()?.message ?: "Failed to save. Please try again."
        }
    }
}

@Composable
private fun OnboardingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
private fun StyleChips(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf(
        "Gentle" to "Soft reminders",
        "Coach" to "Motivating & structured",
        "Direct" to "Short & firm"
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { (label, desc) ->
            val isSelected = selected == label
            ElevatedCard(
                onClick = { onSelected(label) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = label, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Text("✓", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}