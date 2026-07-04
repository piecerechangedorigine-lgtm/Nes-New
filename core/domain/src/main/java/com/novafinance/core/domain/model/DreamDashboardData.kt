package com.novafinance.core.domain.model

/**
 * Everything every possible Dashboard widget could need, computed once
 * by [com.novafinance.core.domain.usecase.GetDreamDashboardDataUseCase].
 * Deliberately eager/combined rather than lazily fetched per visible
 * widget — Nova's data scale is small enough (see the existing
 * "thousands of rows, not millions" note on Analytics) that computing
 * data for a hidden widget is cheap, and it keeps every widget
 * composable a pure render of data it's simply handed rather than each
 * one injecting its own use cases.
 */
data class DreamDashboardData(
    val summary: DashboardSummary,
    val forecast: ForecastSummary,
    val balanceOverview: BalanceOverview,
    val goalForecasts: List<GoalForecast>,
    val topInsightMessage: String,
    val topInsightAction: AssistantAction?,
    val debtSummary: DebtSummary,
    val topDebtRecommendation: String?,
    /** Active sources only — for the Source Health widget (11.11) to show names alongside scores. */
    val sources: List<FinancialSource>,
    val sourceHealths: List<SourceHealth>
)
