package com.novafinance.feature.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.AnalyticsSummary
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.usecase.GetAnalyticsSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getAnalyticsSummary: GetAnalyticsSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getAnalyticsSummary(Unit).collect { result ->
                _uiState.value = when (result) {
                    is NovaResult.Success -> AnalyticsUiState(isLoading = false, summary = result.data)
                    is NovaResult.Error -> AnalyticsUiState(isLoading = false, errorMessage = result.failure.message)
                    is NovaResult.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }
}

@Immutable
data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val summary: AnalyticsSummary? = null,
    val errorMessage: String? = null
)
