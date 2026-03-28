package com.qian.scrollsanity.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qian.scrollsanity.domain.model.session.TrackedAppId
import com.qian.scrollsanity.domain.model.user.GoalItem
import com.qian.scrollsanity.domain.model.user.InterestItem
import com.qian.scrollsanity.domain.model.user.UserPreferences
import com.qian.scrollsanity.domain.model.user.UserProfile
import com.qian.scrollsanity.domain.repo.AuthRepo
import com.qian.scrollsanity.domain.usecase.user.AddGoalUseCase
import com.qian.scrollsanity.domain.usecase.user.AddInterestUseCase
import com.qian.scrollsanity.domain.usecase.user.DeleteGoalUseCase
import com.qian.scrollsanity.domain.usecase.user.DeleteInterestUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveGoalsUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveInterestsUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveUserPreferencesUseCase
import com.qian.scrollsanity.domain.usecase.user.ObserveUserProfileUseCase
import com.qian.scrollsanity.domain.usecase.user.SetGoalAchievedUseCase
import com.qian.scrollsanity.domain.usecase.user.SetInterestAchievedUseCase
import com.qian.scrollsanity.domain.usecase.user.SetTrackedAppEnabledUseCase
import com.qian.scrollsanity.domain.usecase.user.SyncUserPreferencesFromRemoteUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateDisplayNameUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateInterventionIntensityUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateNicknameUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateNotificationsEnabledUseCase
import com.qian.scrollsanity.domain.usecase.user.UpdateToneStyleUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepo: AuthRepo,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val updateNicknameUseCase: UpdateNicknameUseCase,
    private val updateDisplayNameUseCase: UpdateDisplayNameUseCase,
    private val observeUserPreferencesUseCase: ObserveUserPreferencesUseCase,
    private val updateInterventionIntensityUseCase: UpdateInterventionIntensityUseCase,
    private val updateToneStyleUseCase: UpdateToneStyleUseCase,
    private val updateNotificationsEnabledUseCase: UpdateNotificationsEnabledUseCase,
    private val setTrackedAppEnabledUseCase: SetTrackedAppEnabledUseCase,
    private val syncUserPreferencesFromRemoteUseCase: SyncUserPreferencesFromRemoteUseCase,
    private val observeGoalsUseCase: ObserveGoalsUseCase,
    private val addGoalUseCase: AddGoalUseCase,
    private val setGoalAchievedUseCase: SetGoalAchievedUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val observeInterestsUseCase: ObserveInterestsUseCase,
    private val addInterestUseCase: AddInterestUseCase,
    private val setInterestAchievedUseCase: SetInterestAchievedUseCase,
    private val deleteInterestUseCase: DeleteInterestUseCase
) : ViewModel() {

    private val userId: String?
        get() = authRepo.getCurrentUserId()

    init {
        viewModelScope.launch {
            syncUserPreferencesFromRemoteUseCase()
        }
    }

    val profile: StateFlow<UserProfile?> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(observeUserProfileUseCase(uid))
            } else {
                emit(null)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val preferences: StateFlow<UserPreferences?> =
        observeUserPreferencesUseCase()
            .map<UserPreferences, UserPreferences?> { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

    val interventionIntensity: StateFlow<String> =
        preferences
            .map { prefs -> prefs?.interventionIntensity ?: "MEDIUM" }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "MEDIUM"
            )

    val toneStyle: StateFlow<String> =
        preferences
            .map { prefs -> prefs?.toneStyle ?: "gentle" }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "gentle"
            )

    val notificationsEnabled: StateFlow<Boolean> =
        preferences
            .map { prefs -> prefs?.notificationsEnabled ?: true }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = true
            )

    val enabledTrackedApps: StateFlow<Set<TrackedAppId>> =
        preferences
            .map { prefs ->
                prefs?.enabledTrackedApps
                    ?.mapNotNull { raw ->
                        runCatching { TrackedAppId.valueOf(raw) }.getOrNull()
                    }
                    ?.toSet()
                    ?: emptySet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    val goals: StateFlow<List<GoalItem>> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(observeGoalsUseCase(uid))
            } else {
                emit(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val interests: StateFlow<List<InterestItem>> =
        flow {
            val uid = userId
            if (uid != null) {
                emitAll(observeInterestsUseCase(uid))
            } else {
                emit(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

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

    fun updateNickname(nickname: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            updateNicknameUseCase(uid, nickname)
        }
    }

    fun updateDisplayName(displayName: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            updateDisplayNameUseCase(uid, displayName)
        }
    }

    fun updateInterventionIntensity(intensity: String) {
        viewModelScope.launch {
            updateInterventionIntensityUseCase(intensity)
        }
    }

    fun updateToneStyle(style: String) {
        viewModelScope.launch {
            updateToneStyleUseCase(style)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            updateNotificationsEnabledUseCase(enabled)
        }
    }

    fun setTrackedAppEnabled(id: TrackedAppId, enabled: Boolean) {
        viewModelScope.launch {
            setTrackedAppEnabledUseCase(id, enabled)
        }
    }

    fun addGoal(text: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            addGoalUseCase(uid, text)
        }
    }

    fun setGoalAchieved(goalId: String, achieved: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            setGoalAchievedUseCase(uid, goalId, achieved)
        }
    }

    fun deleteGoal(goalId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            deleteGoalUseCase(uid, goalId)
        }
    }

    fun addInterest(text: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            addInterestUseCase(uid, text)
        }
    }

    fun setInterestAchieved(interestId: String, achieved: Boolean) {
        val uid = userId ?: return
        viewModelScope.launch {
            setInterestAchievedUseCase(uid, interestId, achieved)
        }
    }

    fun deleteInterest(interestId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            deleteInterestUseCase(uid, interestId)
        }
    }
}