package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.calculateDebtFreedomDate
import com.novafinance.core.domain.model.calculateDebtHealth
import com.novafinance.core.domain.model.calculateDebtPressure
import com.novafinance.core.domain.model.calculateDebtWeather
import com.novafinance.core.domain.repository.DebtRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Composes [DebtRepository] with the same income figure Dashboard
 * already shows ([GetDashboardSummaryUseCase]'s `monthIncome`) and the
 * same liquidity figure Phase 8.5 already computes
 * ([GetBalanceOverviewUseCase]'s `totalLiquidity`) — Debt Health and
 * Debt Pressure never compute their own version of either number.
 */
class GetDebtSummaryUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getBalanceOverview: GetBalanceOverviewUseCase,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, DebtSummary>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<DebtSummary>> =
        combine(
            debtRepository.observeDebts(),
            getDashboardSummary(Unit),
            getBalanceOverview(Unit)
        ) { debts, summaryResult, overviewResult ->
            val results: List<NovaResult<*>> = listOf(summaryResult, overviewResult)
            val firstError = results.filterIsInstance<NovaResult.Error>().firstOrNull()
            when {
                firstError != null -> firstError
                results.any { it is NovaResult.Loading } -> NovaResult.Loading
                else -> {
                    val monthlyIncome = (summaryResult as NovaResult.Success).data.monthIncome
                    val totalLiquidity = (overviewResult as NovaResult.Success).data.totalLiquidity

                    val health = calculateDebtHealth(debts, monthlyIncome, totalLiquidity)
                    val weather = calculateDebtWeather(debts, health)
                    val pressure = calculateDebtPressure(debts, monthlyIncome, totalLiquidity)
                    val freedom = calculateDebtFreedomDate(debts)

                    val owed = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
                    val owedToMe = debts.filter { it.direction == DebtDirection.OWED_TO_ME && it.isActive && !it.isPaidOff }

                    NovaResult.Success(
                        DebtSummary(
                            debts = debts,
                            totalOwed = Money.sum(owed.map { it.currentBalance }),
                            totalOwedToMe = Money.sum(owedToMe.map { it.currentBalance }),
                            health = health,
                            weather = weather,
                            pressure = pressure,
                            freedomProjection = freedom
                        )
                    )
                }
            }
        }
}
