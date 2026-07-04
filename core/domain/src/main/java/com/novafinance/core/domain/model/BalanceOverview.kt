package com.novafinance.core.domain.model

/**
 * The fuller balance picture Dashboard's simple "sum every active
 * source" total deliberately doesn't attempt (see
 * GetDashboardSummaryUseCase — that one stays a simple headline number
 * on purpose). This is where liabilities and goal-earmarked money
 * actually get accounted for.
 *
 * This is Phase 11.3's "Balance Intelligence Engine" — the type and
 * its use case (`GetBalanceOverviewUseCase`) kept their Phase 8.5 names
 * rather than being renamed to match the brief's phrasing exactly,
 * specifically to avoid a purely cosmetic rename touching every
 * existing consumer (Dashboard, the Financial Overview widget, the
 * home screen widget, Debt Health/Pressure). See
 * `FINANCIAL_SOURCES_ARCHITECTURE.md` for the full reasoning — the
 * same pragmatic call Phase 8.5 made keeping `Transaction.accountId`'s
 * name after the Account → FinancialSource rename.
 */
data class BalanceOverview(
    /** Every active, [FinancialSource.includeInLiquidity] asset source's balance minus every such liability source's balance — real net worth, not just a sum. */
    val totalLiquidity: Money,
    /** Liquidity from [FinancialSource.includeInSpendingPower]-flagged sources only, minus [dreamSafeBalance] — what's actually free to spend day-to-day. A savings account can count toward [totalLiquidity] but not this, per 11.4's own example. */
    val availableSpendingPower: Money,
    /** Total currently saved across every savings goal — "protected" money in the sense that spending it would set a goal back, even though it physically sits in the same accounts as everything else. */
    val dreamSafeBalance: Money,
    /** Sum of every active source the person has explicitly marked [FinancialSource.isEmergencyReserve] — never inferred from a source's name or type. Zero if nothing's been marked. */
    val emergencyReserve: Money,
    /** Projected month-end balance across [FinancialSource.includeInForecast]-flagged sources only, using the same spend-pace projection [com.novafinance.core.domain.usecase.GetForecastSummaryUseCase] already computes for the whole account. */
    val forecastBalance: Money
)
