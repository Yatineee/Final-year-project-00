package com.qian.scrollsanity.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qian.scrollsanity.domain.model.dashboard.LiveDashboardSummary
import com.qian.scrollsanity.domain.session.LiveSessionStateHolder
import com.qian.scrollsanity.domain.usecase.dashboard.GetLiveDashboardSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: LiveDashboardSummary? = null
)

class DashboardViewModel(
    private val getLiveDashboardSummaryUseCase: GetLiveDashboardSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeLiveSession()
    }

    private fun observeLiveSession() {
        viewModelScope.launch {
            LiveSessionStateHolder.state.collect { sessionState ->
                val summary = getLiveDashboardSummaryUseCase(
                    currentSessionMinutes = if (sessionState.inTrackedSession) {
                        sessionState.currentSessionMinutes
                    } else {
                        null
                    },
                    inTrackedSession = sessionState.inTrackedSession
                )

                _uiState.value = DashboardUiState(
                    isLoading = false,
                    summary = summary
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val getLiveDashboardSummaryUseCase: GetLiveDashboardSummaryUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(getLiveDashboardSummaryUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}