package com.novafinance.core.domain.model

/**
 * Everything a Debt widget could need, computed once by
 * [com.novafinance.core.domain.usecase.GetDebtSummaryUseCase] — the
 * same "one combined data source, not four separate use case calls per
 * widget" pattern [DreamDashboardData] already established in Phase 9.
 *
 * Liquidity is read from [BalanceOverview] but never fed back into it
 * — Debt calculations are downstream consumers of liquidity, not a
 * dependency of how liquidity itself is computed. This is a deliberate
 * design choice, not an oversight: making `GetBalanceOverviewUseCase`
 * itself subtract standalone [Debt] balances would create a circular
 * dependency (Debt health needs liquidity; liquidity would need debt).
 * See `DEBT_ARCHITECTURE.md` for the full reasoning and how the
 * Financial Overview widget shows a debt-aware figure anyway without
 * that cycle.
 */
data class DebtSummary(
    val debts: List<Debt>,
    val totalOwed: Money,
    val totalOwedToMe: Money,
    val health: DebtHealthScore,
    val weather: DebtWeather,
    val pressure: DebtPressureScore,
    val freedomProjection: PayoffPlanResult
) {
    val activeOwedDebts: List<Debt> get() = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
    val activeReceivables: List<Debt> get() = debts.filter { it.direction == DebtDirection.OWED_TO_ME && it.isActive && !it.isPaidOff }
}
