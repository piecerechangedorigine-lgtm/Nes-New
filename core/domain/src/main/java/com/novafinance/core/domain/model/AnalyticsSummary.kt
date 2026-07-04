package com.novafinance.core.domain.model

import java.time.YearMonth

/** One category's share of a period's spend — the donut chart's data unit. */
data class CategoryBreakdown(
    val category: TransactionCategory,
    val amount: Money,
    val percentOfTotal: Float
)

/** One month's income vs. expense totals — the bar/line chart's data unit. */
data class MonthlyTotal(
    val month: YearMonth,
    val income: Money,
    val expense: Money
) {
    val net: Money get() = income - expense
}

/**
 * Everything the Analytics screen renders, computed once from the full
 * transaction ledger rather than piecemeal per chart — every widget on
 * the screen reads a slice of the same [AnalyticsSummary] so they can
 * never show numbers that quietly disagree with each other.
 */
data class AnalyticsSummary(
    val currentMonthBreakdown: List<CategoryBreakdown>,
    val monthlyTrend: List<MonthlyTotal>,
    val monthOverMonthExpenseChangePercent: Float?
)
