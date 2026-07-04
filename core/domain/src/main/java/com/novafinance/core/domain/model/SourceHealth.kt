package com.novafinance.core.domain.model

import kotlin.math.roundToInt

data class SourceHealth(val sourceId: String, val score: Int, val label: DebtHealthLabel)

/**
 * A single source's own health, distinct from [BalanceHealthScore]
 * (the whole-portfolio view) — the 11.9 brief's own examples show
 * genuinely different scores per source ("Emergency Fund 95/100,
 * Credit Card 62/100, Checking Account 84/100"), so this can't just be
 * the aggregate score repeated per source.
 *
 * Reuses [DebtHealthLabel] for the label rather than introducing a
 * sixth near-identical four-tier enum — HEALTHY/MODERATE/HIGH_RISK/
 * CRITICAL reads exactly as well for a source's balance health as it
 * does for debt health, and every place a label already needs
 * color/text mapping (see `healthTint` in `DashboardWidgets.kt`) gets
 * it for free rather than needing a parallel mapping function.
 *
 * Two genuinely different calculations depending on source type:
 * - A credit card with a limit set is scored purely on
 *   [CreditCardUtilization] — a card's "health" *is* its utilization,
 *   nothing else about its balance matters to this score. As of Phase
 *   11.5.1, a linked card is scored on its *reconciled* utilization
 *   (via [effectiveCreditCardUtilization]) — the same "the Debt wins
 *   when linked" ownership rule every other liability calculation
 *   follows, so a stale `FinancialSource` balance can't make a card
 *   look healthier than its actual, actively-maintained payoff plan
 *   says it is.
 * - Every other source is scored on how many months of the person's
 *   own average monthly expense its balance could cover — a generic
 *   "buffer" proxy, capped at a comfortable 6-month target. This is
 *   the same style of proxy [DebtPressureScore]'s liquidity-buffer
 *   factor already uses, reused here rather than inventing a third
 *   variant of the same idea.
 */
fun calculateSourceHealth(source: FinancialSource, monthlyExpense: Money, debts: List<Debt> = emptyList()): SourceHealth {
    val utilization = effectiveCreditCardUtilization(source, debts) ?: source.creditCardUtilization
    val score = if (utilization != null) {
        (100 - utilization.utilizationPercent).coerceIn(0, 100)
    } else if (monthlyExpense.minorUnits > 0) {
        val bufferMonths = source.currentBalance.minorUnits.toDouble() / monthlyExpense.minorUnits
        (100 * (bufferMonths / 6.0).coerceIn(0.0, 1.0)).roundToInt()
    } else {
        // No expense data to judge a buffer against — a positive
        // balance with nothing to compare it to reads as a neutral
        // pass rather than a zero, same "missing data isn't a
        // synonym for zero" principle DebtHealthScore already follows.
        if (source.currentBalance.isPositive) 70 else 40
    }

    return SourceHealth(sourceId = source.id, score = score, label = labelFor(score))
}

private fun labelFor(score: Int): DebtHealthLabel = when {
    score >= 80 -> DebtHealthLabel.HEALTHY
    score >= 60 -> DebtHealthLabel.MODERATE
    score >= 35 -> DebtHealthLabel.HIGH_RISK
    else -> DebtHealthLabel.CRITICAL
}
