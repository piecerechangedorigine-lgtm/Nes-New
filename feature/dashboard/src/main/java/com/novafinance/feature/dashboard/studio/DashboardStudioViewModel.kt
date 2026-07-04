package com.novafinance.feature.dashboard.studio

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DashboardPreset
import com.novafinance.core.domain.model.DashboardWidgetConfig
import com.novafinance.core.domain.model.DashboardWidgetType
import com.novafinance.core.domain.model.DreamBackground
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.GoalVisualizationMode
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.WidgetCatalog
import com.novafinance.core.domain.model.WidgetCatalogEntry
import com.novafinance.core.domain.model.WidgetSize
import com.novafinance.core.domain.repository.DashboardRepository
import com.novafinance.core.domain.usecase.ApplyDashboardPresetUseCase
import com.novafinance.core.domain.usecase.DashboardLayoutDefaults
import com.novafinance.core.domain.usecase.GetGoalForecastUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Every mutation here follows the same shape: take the current
 * [DashboardLayout] from [uiState], produce a new widget list, persist
 * it via [dashboardRepository]. There's no optimistic local-only state —
 * [uiState] is a direct projection of what's persisted, so the Studio
 * screen can never show something that isn't actually saved (a
 * `stateIn` restart after backgrounding the app can't lose an edit that
 * was never committed in the first place).
 */
@HiltViewModel
class DashboardStudioViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val getGoalForecast: GetGoalForecastUseCase,
    private val applyDashboardPreset: ApplyDashboardPresetUseCase
) : ViewModel() {

    private val _catalogSearchQuery = MutableStateFlow("")

    val uiState: StateFlow<DashboardStudioUiState> = combine(
        dashboardRepository.observeLayout(),
        getGoalForecast(Unit),
        _catalogSearchQuery
    ) { layout, forecastResult, query ->
        val forecasts = (forecastResult as? NovaResult.Success)?.data ?: emptyList()
        val goalIdsWithWidgets = layout.widgets.filter { it.type == DashboardWidgetType.GOAL }.mapNotNull { it.goalId }.toSet()
        val existingTypes = layout.widgets.filterNot { it.type == DashboardWidgetType.GOAL }.map { it.type }.toSet()

        DashboardStudioUiState(
            isLoading = false,
            layout = layout,
            goalsWithoutWidgets = forecasts.filterNot { it.goal.id in goalIdsWithWidgets },
            hiddenWidgets = layout.widgets.filterNot { it.isVisible },
            // A catalog entry only makes sense to "add" if it isn't
            // already a visible widget — a hidden one shows up in
            // hiddenWidgets (restore) instead of the add catalog
            // (create), since "add" and "restore" are different
            // actions on the same underlying config (see onAddWidget).
            catalogQuery = query,
            catalogResults = WidgetCatalog.search(query).filterNot { entry ->
                entry.type in existingTypes && layout.widgets.first { it.type == entry.type }.isVisible
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardStudioUiState()
    )

    fun onToggleHidden(widgetId: String) {
        updateWidgets { widgets ->
            widgets.map { if (it.id == widgetId) it.copy(isVisible = !it.isVisible) else it }
        }
    }

    fun onRemove(widgetId: String) {
        updateWidgets { widgets -> widgets.filterNot { it.id == widgetId } }
    }

    fun onAddGoalWidget(forecast: GoalForecast) {
        updateWidgets { widgets ->
            val nextOrder = (widgets.maxOfOrNull { it.order } ?: -1) + 1
            widgets + DashboardWidgetConfig(
                id = DashboardLayoutDefaults.goalWidgetId(forecast.goal.id),
                type = DashboardWidgetType.GOAL,
                isVisible = true,
                order = nextOrder,
                goalId = forecast.goal.id
            )
        }
    }

    /**
     * The generic "Add Widget" flow (11.5.4) every non-`GOAL` type goes
     * through — this is the fix for the gap
     * `DASHBOARD_ARCHITECTURE.md`'s postscript flagged back in Phase 9:
     * before this, only goal widgets had any dynamic add path at all,
     * so a person on a preset that hid, say, `DEBT_COACH` by default
     * had no in-app way to add it back. Two cases:
     * - The type already has a (hidden) config from a prior preset or
     *   an earlier removal → just make it visible again, preserving
     *   whatever size/position it already had.
     * - The type has never existed in this layout at all → create a
     *   fresh config at `WidgetSize.MEDIUM`, appended to the end.
     */
    fun onAddWidget(entry: WidgetCatalogEntry) {
        updateWidgets { widgets ->
            val existing = widgets.find { it.type == entry.type }
            if (existing != null) {
                widgets.map { if (it.id == existing.id) it.copy(isVisible = true) else it }
            } else {
                val nextOrder = (widgets.maxOfOrNull { it.order } ?: -1) + 1
                widgets + DashboardWidgetConfig(
                    id = entry.type.name.lowercase(),
                    type = entry.type,
                    isVisible = true,
                    order = nextOrder,
                    size = WidgetSize.MEDIUM
                )
            }
        }
    }

    fun onCatalogSearchChange(query: String) {
        _catalogSearchQuery.value = query
    }

    fun onMoveUp(widgetId: String) = reorder(widgetId, delta = -1)
    fun onMoveDown(widgetId: String) = reorder(widgetId, delta = 1)

    private fun reorder(widgetId: String, delta: Int) {
        updateWidgets { widgets ->
            val ordered = widgets.sortedBy { it.order }.toMutableList()
            val index = ordered.indexOfFirst { it.id == widgetId }
            val targetIndex = index + delta
            if (index < 0 || targetIndex < 0 || targetIndex >= ordered.size) return@updateWidgets widgets
            val moved = ordered.removeAt(index)
            ordered.add(targetIndex, moved)
            ordered.mapIndexed { newOrder, widget -> widget.copy(order = newOrder) }
        }
    }

    fun onResize(widgetId: String, size: WidgetSize) {
        updateWidgets { widgets -> widgets.map { if (it.id == widgetId) it.copy(size = size) else it } }
    }

    fun onVisualizationModeChange(widgetId: String, mode: GoalVisualizationMode) {
        updateWidgets { widgets -> widgets.map { if (it.id == widgetId) it.copy(visualizationMode = mode) else it } }
    }

    fun onApplyPreset(preset: DashboardPreset) {
        viewModelScope.launch { applyDashboardPreset(preset) }
    }

    fun onBackgroundSelected(background: DreamBackground) {
        viewModelScope.launch { dashboardRepository.setBackground(background) }
    }

    private fun updateWidgets(transform: (List<DashboardWidgetConfig>) -> List<DashboardWidgetConfig>) {
        val current = uiState.value.layout ?: return
        viewModelScope.launch {
            dashboardRepository.saveLayout(
                current.copy(
                    widgets = transform(current.widgets),
                    // Any manual edit means this layout no longer matches
                    // the preset it started from — see DashboardPreset's
                    // own doc for why that distinction matters.
                    activePreset = null
                )
            )
        }
    }
}

@Immutable
data class DashboardStudioUiState(
    val isLoading: Boolean = true,
    val layout: DashboardLayout? = null,
    val goalsWithoutWidgets: List<GoalForecast> = emptyList(),
    /** Configs that exist but are currently hidden — the "Restore Hidden Widgets" surface (11.5.4). */
    val hiddenWidgets: List<DashboardWidgetConfig> = emptyList(),
    val catalogQuery: String = "",
    val catalogResults: List<WidgetCatalogEntry> = emptyList()
)
