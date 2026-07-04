package com.novafinance.core.domain.model

/**
 * Everything [com.novafinance.core.domain.assistant.AssistantInsightEngine]
 * needs to answer a question, computed once by GetAssistantContextUseCase
 * from the same use cases Dashboard, Analytics, Budgets, Goals, and (as
 * of Phase 10) Debt already read — the Assistant has no data pipeline
 * of its own and can never see numbers that disagree with what those
 * screens show.
 */
data class AssistantContext(
    val dashboard: DashboardSummary,
    val analytics: AnalyticsSummary,
    val budgetProgress: List<BudgetProgress>,
    val goalForecasts: List<GoalForecast>,
    val debtSummary: DebtSummary
) {
    /** True once there's at least one transaction to reason about — gates the "no data yet" reply. */
    val hasActivity: Boolean get() = dashboard.recentTransactions.isNotEmpty()
}
