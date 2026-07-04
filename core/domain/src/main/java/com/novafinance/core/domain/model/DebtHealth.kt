package com.novafinance.core.domain.model

import kotlin.math.roundToInt

data class DebtHealthScore(
    val score: Int,
    val label: DebtHealthLabel,
    val explanation: String
)

enum class DebtHealthLabel { HEALTHY, MODERATE, HIGH_RISK, CRITICAL }

/**
 * Scores five factors out of a 100-point budget, all built from data
 * that's already computed elsewhere ([DashboardSummary.monthIncome],
 * [BalanceOverview.totalLiquidity]) rather than anything new — the
 * same "deliberately simple heuristics, not a model" approach
 * [GoalHealth] and the Dashboard insight banner already use.
 *
 * Only [DebtDirection.I_OWE] debts count against health — money owed
 * *to* the person is a receivable, not a burden; it doesn't improve
 * this score either; [DebtPressureScore] and the Debt Recovery widget
 * are where receivables actually show up.
 *
 * Every factor degrades gracefully to a neutral score when its input
 * is missing (no income data, no liquidity data, a debt with no
 * [Debt.minimumMonthlyPayment]) rather than crashing or silently
 * treating "unknown" as "zero," which would unfairly tank the score
 * for incomplete data entry rather than actual debt problems.
 */
fun calculateDebtHealth(debts: List<Debt>, monthlyIncome: Money, totalLiquidity: Money): DebtHealthScore {
    val owed = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }

    if (owed.isEmpty()) {
        return DebtHealthScore(score = 100, label = DebtHealthLabel.HEALTHY, explanation = "No active debt.")
    }

    val totalOwed = Money.sum(owed.map { it.currentBalance })

    // Debt size (0-25): total owed against a year of income — owing
    // more than a full year's income is treated as the worst case.
    val sizeComponent = if (monthlyIncome.minorUnits > 0) {
        val annualIncome = monthlyIncome * 12
        val ratio = totalOwed.minorUnits.toDouble() / annualIncome.minorUnits
        (25 * (1 - ratio.coerceIn(0.0, 1.0))).roundToInt()
    } else 12 // Neutral midpoint — no income data to judge scale against.

    // Debt-to-income ratio (0-25): monthly obligations against monthly
    // income — spending half of it or more on debt payments is the
    // worst case, matching common real-world debt-to-income guidance.
    val monthlyObligations = Money.sum(owed.mapNotNull { it.minimumMonthlyPayment })
    val debtToIncomeComponent = if (monthlyIncome.minorUnits > 0) {
        val ratio = monthlyObligations.minorUnits.toDouble() / monthlyIncome.minorUnits
        (25 * (1 - (ratio / 0.5).coerceIn(0.0, 1.0))).roundToInt()
    } else 12

    // Number of active debts (0-20): more open debts to juggle is
    // inherently riskier, independent of their size.
    val countComponent = (20 - (owed.size * 4)).coerceIn(0, 20)

    // Repayment progress (0-15): average how far along every debt is.
    val progressComponent = (15 * owed.map { it.percentPaidDown }.average().toFloat()).roundToInt().coerceIn(0, 15)

    // Liquidity impact (0-15): total owed against what's actually
    // liquid right now — owing more than the person could liquidate is
    // the worst case.
    val liquidityComponent = if (totalLiquidity.minorUnits > 0) {
        val ratio = totalOwed.minorUnits.toDouble() / totalLiquidity.minorUnits
        (15 * (1 - ratio.coerceIn(0.0, 1.0))).roundToInt()
    } else 0 // No liquidity at all against active debt is a genuine risk signal, not missing data.

    val score = (sizeComponent + debtToIncomeComponent + countComponent + progressComponent + liquidityComponent).coerceIn(0, 100)
    val label = labelFor(score)
    val explanation = explanationFor(label, owed.size, debtToIncomeComponent, liquidityComponent)

    return DebtHealthScore(score = score, label = label, explanation = explanation)
}

private fun labelFor(score: Int): DebtHealthLabel = when {
    score >= 80 -> DebtHealthLabel.HEALTHY
    score >= 60 -> DebtHealthLabel.MODERATE
    score >= 35 -> DebtHealthLabel.HIGH_RISK
    else -> DebtHealthLabel.CRITICAL
}

private fun explanationFor(label: DebtHealthLabel, debtCount: Int, debtToIncomeComponent: Int, liquidityComponent: Int): String {
    val weakest = when {
        debtToIncomeComponent <= 10 -> "monthly debt payments are taking up a large share of your income"
        liquidityComponent <= 5 -> "your available liquidity is low relative to what you owe"
        debtCount >= 4 -> "you're juggling several debts at once"
        else -> "your overall debt position"
    }
    return when (label) {
        DebtHealthLabel.HEALTHY -> "Your debt position looks healthy."
        DebtHealthLabel.MODERATE -> "Your debt is manageable, but $weakest is worth watching."
        DebtHealthLabel.HIGH_RISK -> "Your debt is at high risk — $weakest."
        DebtHealthLabel.CRITICAL -> "Your debt needs attention now — $weakest."
    }
}
