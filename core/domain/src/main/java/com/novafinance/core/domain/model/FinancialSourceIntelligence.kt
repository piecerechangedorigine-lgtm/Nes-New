package com.novafinance.core.domain.model

/**
 * Everything Phase 11's intelligence engines produce, combined once —
 * the same "one combined data source, not N separate use case calls
 * per widget" pattern [DreamDashboardData] (Phase 9) and [DebtSummary]
 * (Phase 10) already established.
 */
data class FinancialSourceIntelligence(
    val overview: BalanceOverview,
    val balanceHealth: BalanceHealthScore,
    val sourceHealths: List<SourceHealth>,
    val sourceForecasts: List<SourceForecast>,
    val suggestions: List<BalanceSuggestion>
) {
    fun healthFor(sourceId: String): SourceHealth? = sourceHealths.find { it.sourceId == sourceId }
    fun forecastFor(sourceId: String): SourceForecast? = sourceForecasts.find { it.sourceId == sourceId }
}
