package com.novafinance.core.domain.model

/**
 * Everything the Dashboard hero card and summary row need, computed once
 * by GetDashboardSummaryUseCase from live account + transaction flows
 * rather than re-derived piecemeal in the ViewModel or, worse, in
 * composables.
 */
data class DashboardSummary(
    val totalBalance: Money,
    val monthIncome: Money,
    val monthExpense: Money,
    val recentTransactions: List<Transaction>,
    val insight: DashboardInsight?
)

/**
 * A single computed observation surfaced on the Dashboard (e.g. "You're
 * spending faster than last month"). Structured rather than a raw String
 * so the UI can style it by [tone] without string-matching the message.
 */
data class DashboardInsight(
    val message: String,
    val tone: InsightTone
)

enum class InsightTone { POSITIVE, NEUTRAL, WARNING }
