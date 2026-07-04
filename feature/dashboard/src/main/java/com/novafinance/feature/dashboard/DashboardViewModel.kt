package com.novafinance.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DreamDashboardData
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.repository.DashboardRepository
import com.novafinance.core.domain.repository.ProfileRepository
import com.novafinance.core.domain.usecase.GetDreamDashboardDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The Dream Dashboard (Phase 9) reads from two independent sources that
 * both need to be present before there's anything to render: the
 * financial data itself ([GetDreamDashboardDataUseCase] — one call
 * composing every use case every widget could need, see that class's
 * own doc for why) and the person's widget configuration
 * ([DashboardRepository] — visibility, order, size, per-goal
 * visualization mode, background). Neither drives the other; a
 * [combine] keeps them independently reactive so, for example, editing
 * the layout in Dashboard Studio updates this screen immediately without
 * waiting for the next financial-data recompute.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDreamDashboardData: GetDreamDashboardDataUseCase,
    private val dashboardRepository: DashboardRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getDreamDashboardData(Unit),
                dashboardRepository.observeLayout(),
                profileRepository.observeSettings()
            ) { dataResult, layout, settings ->
                when (dataResult) {
                    is NovaResult.Success -> {
                        // Same "Dashboard insights" toggle from Phase
                        // 8.5 gates the AI Insights widget's content
                        // here too — a person who turned insights off
                        // shouldn't see them resurface just because
                        // they're now packaged as a widget instead of a
                        // banner.
                        val data = if (settings.showDashboardInsights) {
                            dataResult.data
                        } else {
                            dataResult.data.copy(topInsightMessage = "", topInsightAction = null)
                        }
                        DashboardUiState(isLoading = false, data = data, layout = layout, showInsights = settings.showDashboardInsights)
                    }
                    is NovaResult.Error -> DashboardUiState(isLoading = false, errorMessage = dataResult.failure.message)
                    is NovaResult.Loading -> _uiState.value.copy(isLoading = true)
                }
            }.collect { _uiState.value = it }
        }
    }
}

@Immutable
data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: DreamDashboardData? = null,
    val layout: DashboardLayout? = null,
    val showInsights: Boolean = true,
    val errorMessage: String? = null
)
