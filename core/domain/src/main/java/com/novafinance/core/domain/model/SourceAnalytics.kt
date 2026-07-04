package com.novafinance.core.domain.model

import java.time.Instant

data class BalanceDistributionEntry(val type: FinancialSourceType, val balance: Money, val percentOfTotal: Float)
data class SourceAllocationEntry(val source: FinancialSource, val balance: Money, val percentOfTotal: Float)
data class CreditUtilizationEntry(val source: FinancialSource, val utilization: CreditCardUtilization)

/** One point on a trend line — [TrendPoint] doesn't specify a unit; [SourceAnalytics.liquidityTrend]/[savingsGrowthTrend] are both sequences of [Money] amounts over time, oldest first. */
data class TrendPoint(val recordedAt: Instant, val amount: Money)

/**
 * All five analytics types from the 11.5.2 brief. Two
 * (`liquidityTrend`, `savingsGrowthTrend`) depend on
 * [BalanceSnapshot] history — see `FINANCIAL_SOURCES_ARCHITECTURE.md`
 * and `ROADMAP_NEXT.md` for the honest state of that: this phase's
 * `BalanceSnapshotWorker` (11.5.3) is the first thing that actually
 * populates snapshots, so these two trends will be genuinely sparse or
 * empty on a fresh install and grow meaningful over the following
 * days/weeks — not a bug, the expected shape of a system whose history
 * just started being recorded.
 */
data class SourceAnalytics(
    val balanceDistribution: List<BalanceDistributionEntry>,
    val sourceAllocation: List<SourceAllocationEntry>,
    val creditUtilizations: List<CreditUtilizationEntry>,
    val liquidityTrend: List<TrendPoint>,
    val savingsGrowthTrend: List<TrendPoint>
)

/**
 * Pure aggregation over data this app already has — no new balance
 * math invented here, only grouping/sorting/percentage math over
 * [FinancialSource.currentBalance] and the reconciliation-aware
 * [effectiveCreditCardUtilization].
 */
fun calculateSourceAnalytics(
    sources: List<FinancialSource>,
    debts: List<Debt>,
    liquiditySnapshots: List<TrendPoint>,
    savingsSnapshots: List<TrendPoint>
): SourceAnalytics {
    val active = sources.filter { it.isActive }
    val totalBalance = Money.sum(active.map { it.currentBalance })
    val totalMinorUnits = totalBalance.minorUnits.takeIf { it != 0L } ?: 1L

    val balanceDistribution = active
        .groupBy { it.type }
        .map { (type, group) ->
            val balance = Money.sum(group.map { it.currentBalance })
            BalanceDistributionEntry(type, balance, balance.minorUnits.toFloat() / totalMinorUnits)
        }
        .sortedByDescending { it.balance.minorUnits }

    val sourceAllocation = active
        .map { source -> SourceAllocationEntry(source, source.currentBalance, source.currentBalance.minorUnits.toFloat() / totalMinorUnits) }
        .sortedByDescending { it.balance.minorUnits }

    val creditUtilizations = active
        .mapNotNull { source -> effectiveCreditCardUtilization(source, debts)?.let { CreditUtilizationEntry(source, it) } }
        .sortedByDescending { it.utilization.utilizationPercent }

    return SourceAnalytics(
        balanceDistribution = balanceDistribution,
        sourceAllocation = sourceAllocation,
        creditUtilizations = creditUtilizations,
        liquidityTrend = liquiditySnapshots.sortedBy { it.recordedAt },
        savingsGrowthTrend = savingsSnapshots.sortedBy { it.recordedAt }
    )
}
