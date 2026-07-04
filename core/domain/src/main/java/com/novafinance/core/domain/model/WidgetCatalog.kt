package com.novafinance.core.domain.model

/** Matches the 11.5.4 brief's own category list exactly. */
enum class WidgetCatalogCategory(val displayName: String) {
    GOALS("Goals"),
    DEBT("Debt"),
    FORECAST("Forecast"),
    FINANCIAL_SOURCES("Financial Sources"),
    AI("AI"),
    SYSTEM("System")
}

data class WidgetCatalogEntry(
    val type: DashboardWidgetType,
    val displayName: String,
    val description: String,
    val category: WidgetCatalogCategory
)

/**
 * Every widget type that can be added back through a generic "Add
 * Widget" flow — deliberately excludes [DashboardWidgetType.GOAL],
 * which isn't a singleton the way every other type is (a person can
 * have zero, one, or many goal widgets, one per goal). Adding a goal
 * widget goes through `DashboardStudioViewModel.onAddGoalWidget` and
 * the existing "goals without widgets" list instead — see
 * `DASHBOARD_ARCHITECTURE.md`'s domain model section for why that
 * distinction exists at the data-model level, not just here.
 */
object WidgetCatalog {
    val entries: List<WidgetCatalogEntry> = listOf(
        WidgetCatalogEntry(DashboardWidgetType.FORECAST, "Monthly Forecast", "This month's spending-pace projection.", WidgetCatalogCategory.FORECAST),
        WidgetCatalogEntry(DashboardWidgetType.BALANCE_FORECAST, "Balance Forecast", "Month-end projection across forecast-eligible sources.", WidgetCatalogCategory.FORECAST),
        WidgetCatalogEntry(DashboardWidgetType.FINANCIAL_OVERVIEW, "Financial Overview", "Total Liquidity, Spending Power, and Dream Safe Balance.", WidgetCatalogCategory.FINANCIAL_SOURCES),
        WidgetCatalogEntry(DashboardWidgetType.SOURCE_HEALTH, "Source Health", "Your healthiest and most at-risk sources.", WidgetCatalogCategory.FINANCIAL_SOURCES),
        WidgetCatalogEntry(DashboardWidgetType.DEBT_OVERVIEW, "Debt Overview", "Total debt, Debt Health, and your projected debt-free date.", WidgetCatalogCategory.DEBT),
        WidgetCatalogEntry(DashboardWidgetType.DEBT_WEATHER, "Debt Weather", "A glance-level read of your debt health and trend.", WidgetCatalogCategory.DEBT),
        WidgetCatalogEntry(DashboardWidgetType.DEBT_RECOVERY, "Debt Recovery", "Money owed to you and when you expect it back.", WidgetCatalogCategory.DEBT),
        WidgetCatalogEntry(DashboardWidgetType.AI_INSIGHTS, "AI Insights", "The Assistant's top spending insight right now.", WidgetCatalogCategory.AI),
        WidgetCatalogEntry(DashboardWidgetType.DEBT_COACH, "Debt Coach", "The AI Debt Coach's top recommendation right now.", WidgetCatalogCategory.AI),
        WidgetCatalogEntry(DashboardWidgetType.RECENT_ACTIVITY, "Recent Activity", "Your latest transactions at a glance.", WidgetCatalogCategory.SYSTEM)
    )

    fun search(query: String): List<WidgetCatalogEntry> {
        if (query.isBlank()) return entries
        val normalized = query.trim().lowercase()
        return entries.filter {
            it.displayName.lowercase().contains(normalized) || it.category.displayName.lowercase().contains(normalized)
        }
    }
}
