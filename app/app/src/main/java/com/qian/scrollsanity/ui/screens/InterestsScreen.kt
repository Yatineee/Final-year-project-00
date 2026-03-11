package com.qian.scrollsanity.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    // Normalize + dedupe
    val normalized = remember(interests) {
        interests.map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interests", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                                error = null
                                isBusy = true

                                val next = (normalized + text)
                                    .distinctBy { it.lowercase() }

                                viewModel.updateInterests(next)

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

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Text(
                "Your interests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 14.dp)
            ) {
                if (normalized.isEmpty()) {
                    item {
                        Text(
                            "No interests yet. Add a few so interventions can feel more personal.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(normalized, key = { it.lowercase() }) { item ->
                        InterestRow(
                            text = item,
                            onDelete = {
                                if (isBusy) return@InterestRow
                                isBusy = true
                                error = null

                                val next = normalized.filterNot { it.equals(item, ignoreCase = true) }
                                viewModel.updateInterests(next)

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
    text: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
    }
}