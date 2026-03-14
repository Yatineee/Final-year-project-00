package com.qian.scrollsanity.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.model.user.UserProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // =============================
    // PROFILE
    // =============================

    val profile: StateFlow<UserProfile?> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observeUserProfile(uid))
            } else {
                emit(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateNickname(nickname: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateNickname(uid, nickname)
        }
    }

    fun updateDisplayName(displayName: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateDisplayName(uid, displayName)
        }
    }

    // =============================
    // PREFERENCES (DataStore + Firestore sync)
    // =============================

    val interventionIntensity: StateFlow<String> =
        preferencesManager.interventionIntensity
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "MEDIUM")

    val toneStyle: StateFlow<String> =
        preferencesManager.toneStyle
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gentle")

    val interests: StateFlow<List<String>> =
        preferencesManager.interests
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentGoalContext: StateFlow<String?> =
        preferencesManager.recentGoalContext
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateInterventionIntensity(intensity: String) {
        viewModelScope.launch {
            preferencesManager.setInterventionIntensity(intensity)
        }
    }

    fun updateToneStyle(style: String) {
        viewModelScope.launch {
            preferencesManager.setToneStyle(style)
        }
    }

    fun updateInterests(newList: List<String>) {
        viewModelScope.launch {
            preferencesManager.setInterests(newList)
        }
    }

    fun updateRecentGoalContext(text: String?) {
        viewModelScope.launch {
            preferencesManager.setRecentGoalContext(text)
        }
    }

    // =============================
    // GOALS
    // =============================

    val goals: StateFlow<List<GoalItem>> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observeGoals(uid))
            } else {
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(text: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.addGoal(uid, text)
        }
    }

    fun setGoalAchieved(goalId: String, achieved: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.setGoalAchieved(uid, goalId, achieved)
        }
    }

    fun deleteGoal(goalId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.deleteGoal(uid, goalId)
        }
    }
}