package com.novafinance.core.domain.model

import kotlinx.serialization.Serializable

/**
 * The four built-in presets from the 9.2 brief. The percentages in the
 * brief ("40% Dreams / 40% Finance / 20% AI") describe a *feel*, not a
 * literal pixel split — there's no fixed number of widgets to divide
 * pixels between, since the number of GOAL widgets varies with how many
 * goals a person actually has. What a preset concretely controls is:
 *
 * - Which widget types appear at all ([widgetSizeFor] returning `null`
 *   means hidden)
 * - How large each visible type renders ([WidgetSize] — see that enum's
 *   own doc for what's actually implemented per size today)
 * - How many GOAL widgets show at once ([maxGoalWidgets] — `null` means
 *   "one per goal", a number means "show only the most urgent N", where
 *   urgency is the goal with the lowest [GoalHealth.score]; see
 *   `DashboardLayoutDefaults.selectGoalsFor`)
 *
 * Switching presets rebuilds the widget list from scratch using these
 * rules (see `DashboardLayoutDefaults.buildLayout`) — any manual
 * customization made before the switch is discarded, which is why
 * `DashboardRepository.applyPreset` is a distinct, explicit action from
 * ordinary widget edits, not something that could happen by accident.
 */
@Serializable
enum class DashboardPreset(val displayName: String) {
    BALANCED("Balanced"),
    DREAM_FOCUS("Dream Focus"),
    FINANCE_FOCUS("Finance Focus"),
    MINIMAL("Minimal");

    /** `null` return means this widget type is hidden entirely in this preset. */
    fun widgetSizeFor(type: DashboardWidgetType): WidgetSize? = when (this) {
        BALANCED -> when (type) {
            DashboardWidgetType.GOAL -> WidgetSize.MEDIUM
            DashboardWidgetType.FORECAST -> WidgetSize.MEDIUM
            DashboardWidgetType.FINANCIAL_OVERVIEW -> WidgetSize.MEDIUM
            DashboardWidgetType.AI_INSIGHTS -> WidgetSize.MEDIUM
            DashboardWidgetType.RECENT_ACTIVITY -> WidgetSize.SMALL
            DashboardWidgetType.DEBT_OVERVIEW -> WidgetSize.MEDIUM
            DashboardWidgetType.DEBT_WEATHER -> null
            DashboardWidgetType.DEBT_COACH -> null
            DashboardWidgetType.DEBT_RECOVERY -> null
            DashboardWidgetType.SOURCE_HEALTH -> null
            DashboardWidgetType.BALANCE_FORECAST -> null
        }
        DREAM_FOCUS -> when (type) {
            DashboardWidgetType.GOAL -> WidgetSize.LARGE
            DashboardWidgetType.FORECAST -> WidgetSize.MEDIUM
            DashboardWidgetType.FINANCIAL_OVERVIEW -> WidgetSize.SMALL
            DashboardWidgetType.AI_INSIGHTS -> WidgetSize.SMALL
            DashboardWidgetType.RECENT_ACTIVITY -> null
            DashboardWidgetType.DEBT_OVERVIEW -> WidgetSize.SMALL
            DashboardWidgetType.DEBT_WEATHER -> null
            DashboardWidgetType.DEBT_COACH -> null
            DashboardWidgetType.DEBT_RECOVERY -> null
            DashboardWidgetType.SOURCE_HEALTH -> null
            DashboardWidgetType.BALANCE_FORECAST -> WidgetSize.SMALL
        }
        FINANCE_FOCUS -> when (type) {
            DashboardWidgetType.GOAL -> WidgetSize.SMALL
            DashboardWidgetType.FORECAST -> WidgetSize.LARGE
            DashboardWidgetType.FINANCIAL_OVERVIEW -> WidgetSize.LARGE
            DashboardWidgetType.AI_INSIGHTS -> WidgetSize.SMALL
            DashboardWidgetType.RECENT_ACTIVITY -> WidgetSize.MEDIUM
            DashboardWidgetType.DEBT_OVERVIEW -> WidgetSize.LARGE
            DashboardWidgetType.DEBT_WEATHER -> WidgetSize.MEDIUM
            DashboardWidgetType.DEBT_COACH -> WidgetSize.MEDIUM
            DashboardWidgetType.DEBT_RECOVERY -> WidgetSize.SMALL
            DashboardWidgetType.SOURCE_HEALTH -> WidgetSize.MEDIUM
            DashboardWidgetType.BALANCE_FORECAST -> WidgetSize.MEDIUM
        }
        MINIMAL -> when (type) {
            DashboardWidgetType.GOAL -> WidgetSize.SMALL
            DashboardWidgetType.FORECAST -> WidgetSize.MEDIUM
            DashboardWidgetType.FINANCIAL_OVERVIEW -> WidgetSize.MEDIUM
            DashboardWidgetType.AI_INSIGHTS -> null
            DashboardWidgetType.RECENT_ACTIVITY -> null
            DashboardWidgetType.DEBT_OVERVIEW -> WidgetSize.SMALL
            DashboardWidgetType.DEBT_WEATHER -> null
            DashboardWidgetType.DEBT_COACH -> null
            DashboardWidgetType.DEBT_RECOVERY -> null
            DashboardWidgetType.SOURCE_HEALTH -> null
            DashboardWidgetType.BALANCE_FORECAST -> null
        }
    }

    /** `null` means show one widget per goal, uncapped. */
    val maxGoalWidgets: Int?
        get() = when (this) {
            BALANCED, DREAM_FOCUS -> null
            FINANCE_FOCUS -> 2
            MINIMAL -> 1
        }

    /**
     * Where each widget type slots in, left to right / top to bottom.
     * [DashboardWidgetType.GOAL] expands to however many goal widgets
     * [maxGoalWidgets] and the person's actual goal count allow, as a
     * contiguous block at this position — see
     * `DashboardLayoutDefaults.buildWidgets` for the expansion.
     */
    val typeOrder: List<DashboardWidgetType>
        get() = when (this) {
            BALANCED -> listOf(
                DashboardWidgetType.FORECAST,
                DashboardWidgetType.GOAL,
                DashboardWidgetType.DEBT_OVERVIEW,
                DashboardWidgetType.FINANCIAL_OVERVIEW,
                DashboardWidgetType.AI_INSIGHTS,
                DashboardWidgetType.RECENT_ACTIVITY
            )
            DREAM_FOCUS -> listOf(
                DashboardWidgetType.GOAL,
                DashboardWidgetType.FORECAST,
                DashboardWidgetType.BALANCE_FORECAST,
                DashboardWidgetType.AI_INSIGHTS,
                DashboardWidgetType.FINANCIAL_OVERVIEW,
                DashboardWidgetType.DEBT_OVERVIEW,
                DashboardWidgetType.RECENT_ACTIVITY
            )
            FINANCE_FOCUS -> listOf(
                DashboardWidgetType.FINANCIAL_OVERVIEW,
                DashboardWidgetType.BALANCE_FORECAST,
                DashboardWidgetType.SOURCE_HEALTH,
                DashboardWidgetType.DEBT_OVERVIEW,
                DashboardWidgetType.DEBT_WEATHER,
                DashboardWidgetType.FORECAST,
                DashboardWidgetType.RECENT_ACTIVITY,
                DashboardWidgetType.GOAL,
                DashboardWidgetType.DEBT_COACH,
                DashboardWidgetType.DEBT_RECOVERY,
                DashboardWidgetType.AI_INSIGHTS
            )
            MINIMAL -> listOf(
                DashboardWidgetType.FINANCIAL_OVERVIEW,
                DashboardWidgetType.DEBT_OVERVIEW,
                DashboardWidgetType.FORECAST,
                DashboardWidgetType.GOAL,
                DashboardWidgetType.AI_INSIGHTS,
                DashboardWidgetType.RECENT_ACTIVITY
            )
        }
}
