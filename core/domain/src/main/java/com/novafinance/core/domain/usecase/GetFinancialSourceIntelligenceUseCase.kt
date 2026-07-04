package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.FinancialSourceIntelligence
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.calculateBalanceHealth
import com.novafinance.core.domain.model.calculateSourceForecast
import com.novafinance.core.domain.model.calculateSourceHealth
import com.novafinance.core.domain.model.generateBalanceSuggestions
import com.novafinance.core.domain.repository.FinancialSourceRepository
import com.novafinance.core.domain.repository.TransactionRepository
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Composes every Phase 11 engine from use cases and repositories that
 * already existed — no engine here queries a repository Nova didn't
 * already have a use case for. Per-source transactions for
 * [com.novafinance.core.domain.model.calculateSourceForecast] come from
 * one `observeTransactionsForMonth` call, grouped by `accountId`
 * in-memory, rather than one repository query per source — the same
 * "single combined read, not N queries" principle every other
 * aggregate use case in this app already follows.
 *
 * Six sources need combining, one more than `combine`'s typed
 * overloads go up to (5) — the same nested-`combine` workaround
 * `GetDreamDashboardDataUseCase` and `DebtSimulatorViewModel` already use.
 */
class GetFinancialSourceIntelligenceUseCase @Inject constructor(
    private val financialSourceRepository: FinancialSourceRepository,
    private val transactionRepository: TransactionRepository,
    private val getBalanceOverview: GetBalanceOverviewUseCase,
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getDebtSummary: GetDebtSummaryUseCase,
    private val getForecastSummary: GetForecastSummaryUseCase,
    private val getGoalForecast: GetGoalForecastUseCase,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, FinancialSourceIntelligence>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<FinancialSourceIntelligence>> =
        combine(
            combine(financialSourceRepository.observeSources(), transactionRepository.observeTransactionsForMonth(YearMonth.now()), ::Pair),
            combine(getBalanceOverview(Unit), getDashboardSummary(Unit), getDebtSummary(Unit), ::Triple),
            combine(getForecastSummary(Unit), getGoalForecast(Unit), ::Pair)
        ) { sourceInputs, financeResults, otherResults ->
            val (sources, monthTransactions) = sourceInputs
            val (overviewResult, dashboardResult, debtResult) = financeResults
            val (forecastResult, goalsResult) = otherResults

            val results: List<NovaResult<*>> = listOf(overviewResult, dashboardResult, debtResult, forecastResult, goalsResult)
            val firstError = results.filterIsInstance<NovaResult.Error>().firstOrNull()

            when {
                firstError != null -> firstError
                results.any { it is NovaResult.Loading } -> NovaResult.Loading
                else -> {
                    val overview = (overviewResult as NovaResult.Success).data
                    val monthlyExpense = (dashboardResult as NovaResult.Success).data.monthExpense
                    val debtSummary = (debtResult as NovaResult.Success).data
                    val debtHealth = debtSummary.health
                    val forecast = (forecastResult as NovaResult.Success).data
                    val goals = (goalsResult as NovaResult.Success).data

                    val activeSources = sources.filter { it.isActive }
                    val transactionsBySource = monthTransactions.groupBy { it.accountId }

                    val balanceHealth = calculateBalanceHealth(overview, monthlyExpense, debtHealth, forecast)
                    val sourceHealths = activeSources.map { calculateSourceHealth(it, monthlyExpense, debtSummary.debts) }
                    val sourceForecasts = activeSources.map { source ->
                        calculateSourceForecast(source, transactionsBySource[source.id].orEmpty())
                    }
                    val suggestions = generateBalanceSuggestions(activeSources, overview, goals, monthlyExpense, debtSummary.debts)

                    NovaResult.Success(
                        FinancialSourceIntelligence(
                            overview = overview,
                            balanceHealth = balanceHealth,
                            sourceHealths = sourceHealths,
                            sourceForecasts = sourceForecasts,
                            suggestions = suggestions
                        )
                    )
                }
            }
        }
}
