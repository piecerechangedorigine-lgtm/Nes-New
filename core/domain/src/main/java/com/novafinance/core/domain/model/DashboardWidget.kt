package com.novafinance.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Every distinct card the Dashboard can render. Deliberately a closed
 * enum rather than a plugin/registry system — Phase 9 explicitly scopes
 * out the more speculative widget types (Financial Twin, SMS/OCR status,
 * etc.) that a real plugin architecture would need to justify; adding a
 * new type here is a small, contained change until that changes.
 */
@Serializable
enum class DashboardWidgetType(val category: WidgetCategory, val displayName: String) {
    GOAL(WidgetCategory.DREAMS, "Goal"),
    FORECAST(WidgetCategory.FINANCE, "Monthly forecast"),
    FINANCIAL_OVERVIEW(WidgetCategory.FINANCE, "Financial overview"),
    RECENT_ACTIVITY(WidgetCategory.FINANCE, "Recent activity"),
    AI_INSIGHTS(WidgetCategory.AI, "AI insights"),
    DEBT_OVERVIEW(WidgetCategory.FINANCE, "Debt overview"),
    DEBT_WEATHER(WidgetCategory.FINANCE, "Debt weather"),
    DEBT_COACH(WidgetCategory.AI, "Debt coach"),
    DEBT_RECOVERY(WidgetCategory.FINANCE, "Debt recovery"),
    SOURCE_HEALTH(WidgetCategory.FINANCE, "Source health"),
    BALANCE_FORECAST(WidgetCategory.FINANCE, "Balance forecast")
}

/**
 * The three axes every Dashboard Preset (9.2) balances between. A
 * widget's [DashboardWidgetType.category] is what a preset's weighting
 * actually operates on — "40% Dreams" means roughly 40% of the widgets
 * a preset includes are DREAMS-category ones, not a literal pixel-area
 * split (see [DashboardPreset] doc for why a literal split isn't what
 * this means in practice).
 */
@Serializable
enum class WidgetCategory { DREAMS, FINANCE, AI }

/**
 * "Resize (future-ready architecture)" from the 9.1 brief — three sizes
 * exist today and every widget composable already branches on this, but
 * every widget currently renders its MEDIUM layout regardless of the
 * value stored here. The persistence and the UI control both work; the
 * per-size layouts for GOAL/FORECAST/etc. are the part still to build.
 * See DASHBOARD_ARCHITECTURE.md.
 */
@Serializable
enum class WidgetSize { SMALL, MEDIUM, LARGE }

/**
 * One widget instance on the Dashboard. [id] is stable across reorders
 * and preset switches — for a [DashboardWidgetType.GOAL] widget it's
 * derived from the goal's own id (see `goalWidgetId` in
 * DashboardLayoutDefaults) so a goal getting a widget doesn't depend on
 * fragile positional matching.
 */
@Serializable
data class DashboardWidgetConfig(
    val id: String,
    val type: DashboardWidgetType,
    val isVisible: Boolean = true,
    val order: Int,
    val size: WidgetSize = WidgetSize.MEDIUM,
    /** Only meaningful for [DashboardWidgetType.GOAL] — which goal this card tracks. */
    val goalId: String? = null,
    /** Only meaningful for [DashboardWidgetType.GOAL] — see [GoalVisualizationMode]. */
    val visualizationMode: GoalVisualizationMode = GoalVisualizationMode.RING
)

/**
 * The full, persisted Dashboard configuration for one person. This is
 * what `DashboardRepository` reads and writes as a whole — see that
 * interface's own doc for why widget-list mutations go through it
 * wholesale rather than granular per-widget setters.
 */
@Serializable
data class DashboardLayout(
    val widgets: List<DashboardWidgetConfig>,
    /** Which built-in preset this layout currently matches, if any — null once the person customizes past what any preset describes. See [DashboardPreset.matches]. */
    val activePreset: DashboardPreset?,
    val background: DreamBackground = DreamBackground.None
)

/**
 * Five ways to render a single [DashboardWidgetType.GOAL] card (9.4).
 * [GoalVisualizationMode] is intentionally just a rendering choice, not
 * a data shape — every mode reads the exact same [GoalForecast] +
 * [GoalHealth] a [DashboardWidgetType.GOAL] widget already has, so
 * adding a sixth mode later is purely a new `@Composable` branch, never
 * a data-layer change. See `GoalWidgetRenderer` in `core:designsystem`.
 */
@Serializable
enum class GoalVisualizationMode(val displayName: String, val description: String) {
    RING("Ring", "A circular progress ring — the most compact, information-dense view."),
    TIMELINE("Timeline", "A horizontal bar from today to your target date, with progress marked along it."),
    HORIZON("Horizon", "Progress as a rising horizon line — how close you are to reaching the top."),
    VELOCITY("Velocity", "Your current contribution pace against the pace you actually need."),
    WEATHER("Weather", "A single glance at your goal's health, expressed as weather — sunny when it's healthy, stormy when it needs attention.")
}
