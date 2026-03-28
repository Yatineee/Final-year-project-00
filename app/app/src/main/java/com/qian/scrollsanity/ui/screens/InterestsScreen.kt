package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val interests by viewModel.interests.collectAsStateWithLifecycle()

    var newInterest by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    /**
     * Clean and deduplicate interest items for stable UI rendering.
     *
     * Rules:
     * - trim text
     * - drop blank items
     * - dedupe case-insensitively
     */
    val normalized = remember(interests) {
        interests
            .mapNotNull { interest ->
                val cleanedText = interest.text.trim()
                if (cleanedText.isEmpty()) null else interest.copy(text = cleanedText)
            }
            .distinctBy { it.text.lowercase() }
    }

    val active = normalized.filter { it.status.lowercase() != "achieved" }
    val achieved = normalized.filter { it.status.lowercase() == "achieved" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interests", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAdd = !showAdd }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Add Interest Card (expand/collapse)
            if (showAdd) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newInterest,
                            onValueChange = { newInterest = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("New interest") }
                        )

                        Button(
                            onClick = {
                                val text = newInterest.trim()
                                if (text.isEmpty() || isBusy) return@Button

                                val exists = normalized.any {
                                    it.text.equals(text, ignoreCase = true)
                                }

                                if (exists) {
                                    error = "This interest already exists."
                                    return@Button
                                }

                                error = null
                                isBusy = true

                                viewModel.addInterest(text)

                                isBusy = false
                                newInterest = ""
                                showAdd = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newInterest.trim().isNotEmpty() && !isBusy
                        ) {
                            Text(if (isBusy) "Adding..." else "Add interest")
                        }
                    }
                }
            }

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Active",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                if (active.isEmpty()) {
                    item {
                        Text(
                            text = "No active interests. Add one to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(active, key = { it.id }) { interest ->
                        InterestRow(
                            interest = interest,
                            onToggleAchieved = { achievedNow ->
                                if (isBusy) return@InterestRow
                                isBusy = true
                                error = null

                                viewModel.setInterestAchieved(
                                    interestId = interest.id,
                                    achieved = achievedNow
                                )

                                isBusy = false
                            },
                            onDelete = null
                        )
                    }
                }

                item { Spacer(Modifier.height(10.dp)) }

                item {
                    Text(
                        text = "Achieved",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (achieved.isEmpty()) {
                    item {
                        Text(
                            text = "Nothing achieved yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(achieved, key = { it.id }) { interest ->
                        InterestRow(
                            interest = interest,
                            onToggleAchieved = { achievedNow ->
                                if (isBusy) return@InterestRow
                                isBusy = true
                                error = null

                                viewModel.setInterestAchieved(
                                    interestId = interest.id,
                                    achieved = achievedNow
                                )

                                isBusy = false
                            },
                            onDelete = {
                                if (isBusy) return@InterestRow
                                isBusy = true
                                error = null

                                viewModel.deleteInterest(interest.id)

                                isBusy = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InterestRow(
    interest: InterestItem,
    onToggleAchieved: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val isAchieved = interest.status.lowercase() == "achieved"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAchieved,
                onCheckedChange = { onToggleAchieved(it) }
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = interest.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}