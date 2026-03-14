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
    var recentGoalContext by remember { mutableStateOf("") }
    var goalInput by remember { mutableStateOf("") }
    var interestInput by remember { mutableStateOf("") }

    var toneStyle by remember { mutableStateOf("gentle") }
    var interventionIntensity by remember { mutableStateOf("MEDIUM") }

    var isSaving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val canSubmit = nickname.isNotBlank() && !isSaving && userId != null

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
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                )
            )
        }

        OnboardingCard(title = "What are you working toward?") {
            OutlinedTextField(
                value = recentGoalContext,
                onValueChange = { recentGoalContext = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Recent goal context (e.g. prepare IELTS)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
        }

        OnboardingCard(title = "Add an initial goal") {
            OutlinedTextField(
                value = goalInput,
                onValueChange = { goalInput = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Goal (e.g. No TikTok after 11pm)") }
            )
        }

        OnboardingCard(title = "Your interests") {
            OutlinedTextField(
                value = interestInput,
                onValueChange = { interestInput = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Comma-separated (music, reading, fitness)") }
            )
        }

        OnboardingCard(title = "Preferred coaching style") {
            StyleChips(
                selected = toneStyle,
                onSelected = { toneStyle = it }
            )
        }

        OnboardingCard(title = "Intervention intensity") {
            IntensityChips(
                selected = interventionIntensity,
                onSelected = { interventionIntensity = it }
            )
        }

        errorMsg?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = {
                if (userId == null) {
                    errorMsg = "Not signed in."
                    return@Button
                }
                isSaving = true
                errorMsg = null
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

    LaunchedEffect(isSaving) {
        if (!isSaving) return@LaunchedEffect
        if (userId == null) return@LaunchedEffect

        val goals = listOf(goalInput.trim()).filter { it.isNotBlank() }

        val interests = interestInput
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val input = OnboardingInput(
            nickname = nickname.trim(),
            toneStyle = toneStyle,
            interventionIntensity = interventionIntensity,
            recentGoalContext = recentGoalContext.ifBlank { null },
            recentInterestContext = interests.takeIf { it.isNotEmpty() }?.joinToString(", "),
            goals = goals,
            interests = interests
        )

        val result = firestoreRepo.completeOnboarding(userId, input)

        isSaving = false

        if (result.isSuccess) {
            onFinished()
        } else {
            errorMsg = result.exceptionOrNull()?.message ?: "Failed to save."
        }
    }
}

@Composable
private fun OnboardingCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun StyleChips(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("gentle", "coach", "direct")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            ElevatedCard(
                onClick = { onSelected(option) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = option.replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f)
                    )

                    if (selected == option) {
                        Text("✓")
                    }
                }
            }
        }
    }
}

@Composable
private fun IntensityChips(
    selected: String,
    onSelected: (String) -> Unit
) {
    val options = listOf("LOW", "MEDIUM", "HIGH")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { option ->
            ElevatedCard(
                onClick = { onSelected(option) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = option,
                        modifier = Modifier.weight(1f)
                    )

                    if (selected == option) {
                        Text("✓")
                    }
                }
            }
        }
    }
}