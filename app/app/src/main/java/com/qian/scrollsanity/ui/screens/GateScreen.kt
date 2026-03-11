package com.qian.scrollsanity.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.FirestoreRepository

@Composable
fun GateScreen(
    onGoLogin: () -> Unit,
    onGoOnboarding: () -> Unit,
    onGoMain: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val repo = remember { FirestoreRepository() }

    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user == null) {
            onGoLogin()
            return@LaunchedEffect
        }

        val profileRes = repo.getUserProfile(user.uid)
        val completed = profileRes.getOrNull()?.onboardingCompleted == true

        if (completed) onGoMain() else onGoOnboarding()
    }

    // 简单 loading
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading...")
    }
}