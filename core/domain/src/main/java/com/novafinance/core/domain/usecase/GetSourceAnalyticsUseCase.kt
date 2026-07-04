package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.SourceAnalytics
import com.novafinance.core.domain.model.TrendPoint
import com.novafinance.core.domain.model.calculateSourceAnalytics
import com.novafinance.core.domain.repository.BalanceSnapshotRepository
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.repository.FinancialSourceRepository
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Composes [FinancialSourceRepository], [DebtRepository] (for
 * reconciliation-aware utilization — see `calculateSourceAnalytics`),
 * and [BalanceSnapshotRepository] into the five analytics types
 * (11.5.2). The two trend charts are day-bucketed sums over whatever
 * snapshot history exists — see `SourceAnalytics`'s own doc for why
 * they're expected to be sparse on a fresh install.
 */
class GetSourceAnalyticsUseCase @Inject constructor(
    private val financialSourceRepository: FinancialSourceRepository,
    private val debtRepository: DebtRepository,
    private val balanceSnapshotRepository: BalanceSnapshotRepository,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, SourceAnalytics>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<SourceAnalytics>> =
        combine(
            financialSourceRepository.observeSources(),
            debtRepository.observeDebts(),
            balanceSnapshotRepository.observeAllSnapshots()
        ) { sources, debts, snapshots ->
            val zone = ZoneId.systemDefault()
            val byDay = snapshots.groupBy { it.recordedAt.atZone(zone).toLocalDate() }

            val liquidityTrend = byDay.entries.sortedBy { it.key }.map { (day, daySnapshots) ->
                TrendPoint(recordedAt = day.atStartOfDay(zone).toInstant(), amount = Money.sum(daySnapshots.map { it.balance }))
            }

            val savingsSourceIds = sources.filter { it.type == FinancialSourceType.SAVINGS_ACCOUNT }.map { it.id }.toSet()
            val savingsGrowthTrend = byDay.entries.sortedBy { it.key }.map { (day, daySnapshots) ->
                val savingsOnly = daySnapshots.filter { it.sourceId in savingsSourceIds }
                TrendPoint(recordedAt = day.atStartOfDay(zone).toInstant(), amount = Money.sum(savingsOnly.map { it.balance }))
            }

            NovaResult.Success(calculateSourceAnalytics(sources, debts, liquidityTrend, savingsGrowthTrend))
        }
}
