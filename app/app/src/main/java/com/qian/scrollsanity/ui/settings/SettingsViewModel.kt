package com.qian.scrollsanity.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.qian.scrollsanity.data.config.FirestoreRepository
import com.qian.scrollsanity.data.perferences.PreferencesManager
import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.model.user.UserProfile
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * SettingsViewModel
 *
 * Responsibilities:
 * 1. Expose profile, preferences, goals, and interests for the signed-in user.
 * 2. Read account-bound data from Firestore using the current FirebaseAuth uid.
 * 3. Keep Settings UI aligned with the actual signed-in account.
 * 4. Write profile / preference / goal / interest changes back to Firestore.
 *
 * Data source design:
 * - Profile: users/{uid}/data/profile
 * - Preferences: users/{uid}/data/preferences
 * - Goals: users/{uid}/goals/{goalId}
 * - Interests: users/{uid}/interests/{interestId}
 *
 * Important design note:
 * - This ViewModel no longer treats recentGoalContext / recentInterestContext
 *   as primary UI data.
 * - Settings and related screens should directly read from goals and interests.
 */
class SettingsViewModel(
    private val firestoreRepo: FirestoreRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    /**
     * Returns the uid of the currently signed-in Firebase user.
     * Returns null when there is no authenticated user.
     */
    private val userId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    // =====================================================
    // PROFILE
    // =====================================================

    /**
     * Live profile document for the current account.
     *
     * Source:
     * users/{uid}/data/profile
     */
    val profile: StateFlow<UserProfile?> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observeUserProfile(uid))
            } else {
                emit(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Updates the in-app nickname stored in the profile document.
     */
    fun updateNickname(nickname: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateNickname(uid, nickname.trim())
        }
    }

    /**
     * Updates the display name stored in the profile document.
     */
    fun updateDisplayName(displayName: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateDisplayName(uid, displayName.trim())
        }
    }

    // =====================================================
    // PREFERENCES
    // =====================================================

    /**
     * Live preferences document for the current account.
     *
     * Source:
     * users/{uid}/data/preferences
     */
    val preferences: StateFlow<UserPreferences?> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observePreferences(uid))
            } else {
                emit(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    /**
     * Current intervention intensity.
     *
     * Falls back to MEDIUM when no preferences document exists yet.
     */
    val interventionIntensity: StateFlow<String> =
        preferences
            .map { prefs -> prefs?.interventionIntensity ?: "MEDIUM" }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "MEDIUM"
            )

    /**
     * Current intervention tone style.
     *
     * Falls back to "gentle" when no preferences document exists yet.
     */
    val toneStyle: StateFlow<String> =
        preferences
            .map { prefs -> prefs?.toneStyle ?: "gentle" }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "gentle"
            )

    /**
     * Updates intervention intensity in Firestore.
     *
     * Also mirrors the value into local DataStore so non-account-specific
     * fallback/cache state stays aligned.
     */
    fun updateInterventionIntensity(intensity: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateInterventionIntensity(uid, intensity)
            preferencesManager.setInterventionIntensity(intensity)
        }
    }

    /**
     * Updates tone style in Firestore.
     *
     * Also mirrors the value into local DataStore.
     */
    fun updateToneStyle(style: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.updateToneStyle(uid, style)
            preferencesManager.setToneStyle(style)
        }
    }

    // =====================================================
    // GOALS
    // =====================================================

    /**
     * Live goal list for the current user.
     *
     * Source:
     * users/{uid}/goals/{goalId}
     */
    val goals: StateFlow<List<GoalItem>> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observeGoals(uid))
            } else {
                emit(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Adds a goal document for the current user.
     */
    fun addGoal(text: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.addGoal(uid, text)
        }
    }

    /**
     * Switches a goal between active and achieved.
     */
    fun setGoalAchieved(goalId: String, achieved: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.setGoalAchieved(uid, goalId, achieved)
        }
    }

    /**
     * Deletes a goal document.
     */
    fun deleteGoal(goalId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.deleteGoal(uid, goalId)
        }
    }

    // =====================================================
    // INTERESTS
    // =====================================================

    /**
     * Live interest list for the current user.
     *
     * Source:
     * users/{uid}/interests/{interestId}
     */
    val interests: StateFlow<List<InterestItem>> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(firestoreRepo.observeInterests(uid))
            } else {
                emit(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Adds an interest document for the current user.
     */
    fun addInterest(text: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.addInterest(uid, text)
        }
    }

    /**
     * Switches an interest between active and achieved.
     */
    fun setInterestAchieved(interestId: String, achieved: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.setInterestAchieved(uid, interestId, achieved)
        }
    }

    /**
     * Deletes an interest document.
     */
    fun deleteInterest(interestId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            firestoreRepo.deleteInterest(uid, interestId)
        }
    }

    // =====================================================
    // DERIVED HELPERS
    // =====================================================

    /**
     * Derived helper for places that want a single quick goal preview.
     *
     * This is computed from the real goals list instead of relying on
     * recentGoalContext as stored source-of-truth.
     */
    val firstActiveGoalText: StateFlow<String?> =
        goals
            .map { items ->
                items.firstOrNull { it.status.equals("active", ignoreCase = true) }?.text
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    /**
     * Derived helper for places that want a short interest preview string.
     *
     * This is computed from the real interests list instead of relying on
     * recentInterestContext as stored source-of-truth.
     */
    val activeInterestPreview: StateFlow<String?> =
        interests
            .map { items ->
                val activeTexts = items
                    .filter { it.status.equals("active", ignoreCase = true) }
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
                    .take(3)

                if (activeTexts.isEmpty()) null else activeTexts.joinToString(", ")
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
}