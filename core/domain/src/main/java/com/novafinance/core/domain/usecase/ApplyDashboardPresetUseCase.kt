package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.DashboardPreset
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.repository.DashboardRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Rebuilds the widget list from scratch for [preset] and persists it —
 * "switch preset" is a deliberate, explicit action distinct from
 * ordinary widget-visibility/order edits (see [DashboardRepository]'s
 * doc comment), which is why this is its own use case rather than
 * `DashboardStudioViewModel` calling `DashboardLayoutDefaults` directly
 * and saving the result itself. The existing background is preserved —
 * switching presets is about widget composition, not the background
 * image someone already picked.
 */
class ApplyDashboardPresetUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val getGoalForecast: GetGoalForecastUseCase,
    dispatcher: CoroutineDispatcher
) : NovaUseCase<DashboardPreset, Unit>(dispatcher) {

    override suspend fun execute(params: DashboardPreset) {
        val forecasts = when (val result = getGoalForecast(Unit).first()) {
            is NovaResult.Success -> result.data
            else -> emptyList()
        }
        val currentLayout = dashboardRepository.observeLayout().first()
        val newLayout = DashboardLayoutDefaults.buildLayout(
            preset = params,
            goalForecasts = forecasts,
            background = currentLayout.background
        )
        dashboardRepository.saveLayout(newLayout)
    }
}
