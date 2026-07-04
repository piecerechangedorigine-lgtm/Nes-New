package com.novafinance.core.domain.model

import java.time.LocalDate

data class SourceForecast(
    val sourceId: String,
    val projectedEndOfMonthBalance: Money,
    val trend: DebtTrend,
    val risk: SourceForecastRisk,
    val message: String
)

/** Reuses [DebtTrend]'s three-value shape (IMPROVING/STABLE/WORSENING) — a source's balance trending up, flat, or down reads identically to a debt's trend, no new enum needed for the same concept. */
enum class SourceForecastRisk { LOW, MODERATE, HIGH }

/**
 * The same linear spend-pace projection
 * [com.novafinance.core.domain.usecase.GetForecastSummaryUseCase]
 * already runs for the whole account (extrapolate this month's
 * per-day pace across the remaining days), scoped down to one source's
 * own transactions instead of every transaction. This is deliberately
 * *not* a second forecasting algorithm — same math, narrower input.
 *
 * [sourceTransactionsThisMonth] should already be filtered to this
 * source's id and the current month by the caller (matching how
 * `TransactionRepository.observeTransactions(accountId)` already
 * supports per-source filtering) — this function does no filtering of
 * its own, only the projection math, to keep it a pure, easily-tested
 * function independent of how the caller sourced its input.
 */
fun calculateSourceForecast(
    source: FinancialSource,
    sourceTransactionsThisMonth: List<Transaction>,
    today: LocalDate = LocalDate.now()
): SourceForecast {
    val daysInMonth = today.lengthOfMonth()
    val daysSoFar = today.dayOfMonth
    val remainingDays = (daysInMonth - daysSoFar).coerceAtLeast(0)

    val netMinorUnits = sourceTransactionsThisMonth.sumOf { it.amount.minorUnits }
    val dailyRate = if (daysSoFar > 0) netMinorUnits.toDouble() / daysSoFar else 0.0
    val projectedAdditional = Money(Math.round(dailyRate * remainingDays))

    val projectedBalance = source.currentBalance + projectedAdditional

    val trend = when {
        dailyRate > 0 -> DebtTrend.IMPROVING
        dailyRate < 0 -> DebtTrend.WORSENING
        else -> DebtTrend.STABLE
    }

    val risk = riskFor(projectedBalance, source.currentBalance)
    val message = "\"${source.name}\" likely reaches ${projectedBalance.formatted()} by month end."

    return SourceForecast(
        sourceId = source.id,
        projectedEndOfMonthBalance = projectedBalance,
        trend = trend,
        risk = risk,
        message = message
    )
}

private fun riskFor(projectedBalance: Money, currentBalance: Money): SourceForecastRisk = when {
    projectedBalance.isNegative -> SourceForecastRisk.HIGH
    projectedBalance.minorUnits < currentBalance.minorUnits / 2 -> SourceForecastRisk.MODERATE
    else -> SourceForecastRisk.LOW
}
