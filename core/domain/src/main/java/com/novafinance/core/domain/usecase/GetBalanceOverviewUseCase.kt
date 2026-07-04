package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.BalanceOverview
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.effectiveLiabilityBalance
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.repository.FinancialSourceRepository
import com.novafinance.core.domain.repository.GoalRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The liability- and goal-aware liquidity calculation the Hybrid
 * Balance Model needs (Phase 8.5.3), extended in Phase 11.3/11.4 to
 * respect each [FinancialSource]'s own inclusion flags, and in Phase
 * 11.5.1 to use [effectiveLiabilityBalance] rather than a liability
 * source's raw [FinancialSource.currentBalance] — a credit card linked
 * to a [Debt] record contributes the *Debt's* balance to every
 * liability-aware calculation here, not its own, independently-editable
 * figure. See `FINANCIAL_SOURCES_ARCHITECTURE.md`'s "Ownership rules"
 * section for the reasoning.
 *
 * Every source's [FinancialSource.balanceUpdateMode] is irrelevant here
 * on purpose — MANUAL, ASSISTED, and SMART sources all feed the same
 * [FinancialSourceRepository] flow this reads, so this calculation
 * doesn't care which mode produced a given balance.
 */
class GetBalanceOverviewUseCase @Inject constructor(
    private val financialSourceRepository: FinancialSourceRepository,
    private val goalRepository: GoalRepository,
    private val debtRepository: DebtRepository,
    private val getForecastSummary: GetForecastSummaryUseCase,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, BalanceOverview>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<BalanceOverview>> =
        combine(
            financialSourceRepository.observeSources(),
            goalRepository.observeGoals(),
            debtRepository.observeDebts(),
            getForecastSummary(Unit)
        ) { sources, goals, debts, forecastResult ->
            when (forecastResult) {
                is NovaResult.Error -> forecastResult
                is NovaResult.Loading -> NovaResult.Loading
                is NovaResult.Success -> NovaResult.Success(buildOverview(sources, goals, debts, forecastResult.data.projectedEndOfMonthBalance))
            }
        }

    private fun buildOverview(sources: List<FinancialSource>, goals: List<SavingsGoal>, debts: List<Debt>, overallProjectedBalance: Money): BalanceOverview {
        val active = sources.filter { it.isActive }

        val totalLiquidity = netBalance(active.filter { it.includeInLiquidity }, debts)
        val spendingPowerLiquidity = netBalance(active.filter { it.includeInSpendingPower }, debts)
        val dreamSafeBalance = Money.sum(goals.map { it.currentAmount })
        val availableSpendingPower = spendingPowerLiquidity - dreamSafeBalance
        val emergencyReserve = Money.sum(active.filter { it.isEmergencyReserve }.map { it.currentBalance })

        // The overall Forecast Engine already computed "how much more
        // will be spent this month" as a single delta
        // (overallProjectedBalance - current total). Reapplying that
        // same delta to just the forecast-eligible subset is a
        // reasonable, honest simplification — it avoids recomputing a
        // second, source-scoped spend-pace calculation that would be
        // easy to let drift from the one the Dashboard's own Forecast
        // widget already shows.
        val overallCurrentTotal = netBalance(active, debts)
        val projectedDelta = overallProjectedBalance - overallCurrentTotal
        val forecastEligibleLiquidity = netBalance(active.filter { it.includeInForecast }, debts)
        val forecastBalance = forecastEligibleLiquidity + projectedDelta

        return BalanceOverview(
            totalLiquidity = totalLiquidity,
            availableSpendingPower = availableSpendingPower,
            dreamSafeBalance = dreamSafeBalance,
            emergencyReserve = emergencyReserve,
            forecastBalance = forecastBalance
        )
    }

    private fun netBalance(sources: List<FinancialSource>, debts: List<Debt>): Money {
        val assets = sources.filter { !it.type.isLiability }
        val liabilities = sources.filter { it.type.isLiability }
        return Money.sum(assets.map { it.currentBalance }) - Money.sum(liabilities.map { effectiveLiabilityBalance(it, debts) })
    }
}
