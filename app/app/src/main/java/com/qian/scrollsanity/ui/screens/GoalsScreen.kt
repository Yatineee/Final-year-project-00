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
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.domain.model.user.GoalItem

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onBack: () -> Unit
) {
    val repo = remember { FirestoreRepository() }
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid }
    val scope = rememberCoroutineScope()

    var newGoal by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    // Observe goals in realtime
    val goals by produceState<List<GoalItem>>(initialValue = emptyList(), key1 = uid) {
        if (uid == null) {
            value = emptyList()
            return@produceState
        }
        repo.observeGoals(uid).collect { value = it }
    }

    val active = goals.filter { it.status != "achieved" }
    val achieved = goals.filter { it.status == "achieved" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.SemiBold) },
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
            if (uid == null) {
                Text(
                    "You are not signed in.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            // Add Goal Card (expand/collapse)
            if (showAdd) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newGoal,
                            onValueChange = { newGoal = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("New goal") }
                        )

                        Button(
                            onClick = {
                                val text = newGoal.trim()
                                if (text.isEmpty() || isBusy) return@Button

                                error = null
                                isBusy = true

                                scope.launch {
                                    val res = repo.addGoal(uid, text)
                                    isBusy = false

                                    if (res.isSuccess) {
                                        newGoal = ""
                                        showAdd = false
                                    } else {
                                        error = res.exceptionOrNull()?.message ?: "Failed to add goal"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newGoal.trim().isNotEmpty() && !isBusy
                        ) {
                            Text(if (isBusy) "Adding..." else "Add goal")
                        }
                    }
                }
            }

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Text(
                "Active",
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
                            "No active goals. Add one to get started.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(active, key = { it.id }) { g ->
                        GoalRow(
                            goal = g,
                            onToggleAchieved = { achievedNow ->
                                if (isBusy) return@GoalRow
                                isBusy = true
                                error = null

                                scope.launch {
                                    val res = repo.setGoalAchieved(uid, g.id, achievedNow)
                                    isBusy = false
                                    if (res.isFailure) {
                                        error = res.exceptionOrNull()?.message ?: "Failed to update goal"
                                    }
                                }
                            },
                            onDelete = null // 你也可以允许删除 active goal
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
                            "Nothing achieved yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(achieved, key = { it.id }) { g ->
                        GoalRow(
                            goal = g,
                            onToggleAchieved = { achievedNow ->
                                if (isBusy) return@GoalRow
                                isBusy = true
                                error = null

                                scope.launch {
                                    val res = repo.setGoalAchieved(uid, g.id, achievedNow)
                                    isBusy = false
                                    if (res.isFailure) {
                                        error = res.exceptionOrNull()?.message ?: "Failed to update goal"
                                    }
                                }
                            },
                            onDelete = {
                                if (isBusy) return@GoalRow
                                isBusy = true
                                error = null

                                scope.launch {
                                    val res = repo.deleteGoal(uid, g.id)
                                    isBusy = false
                                    if (res.isFailure) {
                                        error = res.exceptionOrNull()?.message ?: "Failed to delete goal"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalRow(
    goal: GoalItem,
    onToggleAchieved: (Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    val isAchieved = goal.status == "achieved"

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
            Checkbox(
                checked = isAchieved,
                onCheckedChange = { onToggleAchieved(it) }
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = goal.text,
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