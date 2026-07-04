package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DashboardPreset
import com.novafinance.core.domain.model.DashboardWidgetConfig
import com.novafinance.core.domain.model.DashboardWidgetType
import com.novafinance.core.domain.model.DreamBackground
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.health

/**
 * Turns a [DashboardPreset] plus the person's actual goals into a
 * concrete [DashboardLayout]. Deliberately a plain object with pure
 * functions, not a use case — there's no repository call or dispatcher
 * concern here, just a data transform, so wrapping it in
 * [NovaUseCase]/[NovaFlowUseCase] would add ceremony without adding
 * anything. [ApplyDashboardPresetUseCase] is the thin use case that
 * actually persists the result this produces.
 */
object DashboardLayoutDefaults {

    /** Stable, deterministic id for a goal's widget — never a random UUID, so re-applying a preset doesn't create a duplicate card for the same goal. */
    fun goalWidgetId(goalId: String): String = "goal_$goalId"

    private fun widgetId(type: DashboardWidgetType): String = "widget_${type.name.lowercase()}"

    /**
     * Goals are selected most-urgent-first when a preset caps how many
     * can show (see [DashboardPreset.maxGoalWidgets]) — "urgent" here
     * means the lowest [com.novafinance.core.domain.model.GoalHealth.score],
     * so a Finance Focus or Minimal dashboard still surfaces the goal
     * that most needs attention rather than an arbitrary one.
     */
    fun buildWidgets(preset: DashboardPreset, goalForecasts: List<GoalForecast>): List<DashboardWidgetConfig> {
        val selectedGoals = preset.maxGoalWidgets?.let { cap ->
            goalForecasts.sortedBy { it.health().score }.take(cap)
        } ?: goalForecasts

        val widgets = mutableListOf<DashboardWidgetConfig>()
        var order = 0

        for (type in preset.typeOrder) {
            if (type == DashboardWidgetType.GOAL) {
                val size = preset.widgetSizeFor(DashboardWidgetType.GOAL) ?: continue
                selectedGoals.forEach { forecast ->
                    widgets += DashboardWidgetConfig(
                        id = goalWidgetId(forecast.goal.id),
                        type = DashboardWidgetType.GOAL,
                        isVisible = true,
                        order = order++,
                        size = size,
                        goalId = forecast.goal.id
                    )
                }
            } else {
                val size = preset.widgetSizeFor(type) ?: continue
                widgets += DashboardWidgetConfig(
                    id = widgetId(type),
                    type = type,
                    isVisible = true,
                    order = order++,
                    size = size
                )
            }
        }

        return widgets
    }

    fun buildLayout(preset: DashboardPreset, goalForecasts: List<GoalForecast>, background: DreamBackground = DreamBackground.None): DashboardLayout =
        DashboardLayout(
            widgets = buildWidgets(preset, goalForecasts),
            activePreset = preset,
            background = background
        )

    /** Every new install starts here — Balanced, before the person has touched anything. */
    fun initialLayout(goalForecasts: List<GoalForecast>): DashboardLayout =
        buildLayout(DashboardPreset.BALANCED, goalForecasts)
}
