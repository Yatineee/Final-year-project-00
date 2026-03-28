package com.qian.scrollsanity.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

class SettingsViewModelFactory(
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
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                authRepo = authRepo,
                observeUserProfileUseCase = observeUserProfileUseCase,
                updateNicknameUseCase = updateNicknameUseCase,
                updateDisplayNameUseCase = updateDisplayNameUseCase,
                observeUserPreferencesUseCase = observeUserPreferencesUseCase,
                updateInterventionIntensityUseCase = updateInterventionIntensityUseCase,
                updateToneStyleUseCase = updateToneStyleUseCase,
                updateNotificationsEnabledUseCase = updateNotificationsEnabledUseCase,
                setTrackedAppEnabledUseCase = setTrackedAppEnabledUseCase,
                syncUserPreferencesFromRemoteUseCase = syncUserPreferencesFromRemoteUseCase,
                observeGoalsUseCase = observeGoalsUseCase,
                addGoalUseCase = addGoalUseCase,
                setGoalAchievedUseCase = setGoalAchievedUseCase,
                deleteGoalUseCase = deleteGoalUseCase,
                observeInterestsUseCase = observeInterestsUseCase,
                addInterestUseCase = addInterestUseCase,
                setInterestAchievedUseCase = setInterestAchievedUseCase,
                deleteInterestUseCase = deleteInterestUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}