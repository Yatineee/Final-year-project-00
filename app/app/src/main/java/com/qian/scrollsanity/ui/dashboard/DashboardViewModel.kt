package com.qian.scrollsanity.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qian.scrollsanity.domain.model.dashboard.DashboardSummary
import com.qian.scrollsanity.domain.usecase.dashboard.GetDashboardSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: DashboardSummary? = null
)

class DashboardViewModel(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val summary = getDashboardSummaryUseCase()
            _uiState.value = DashboardUiState(
                isLoading = false,
                summary = summary
            )
        }
    }
}

class DashboardViewModelFactory(
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(getDashboardSummaryUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}