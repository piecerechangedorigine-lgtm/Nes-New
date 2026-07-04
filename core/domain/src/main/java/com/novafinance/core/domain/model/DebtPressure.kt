package com.novafinance.core.domain.model

import kotlin.math.roundToInt

data class DebtPressureScore(val score: Int, val label: DebtPressureLabel)

enum class DebtPressureLabel { LOW, MODERATE, HIGH, EXTREME }

/**
 * Distinct from [DebtHealthScore] on purpose — Health asks "is your
 * overall debt position structurally sound" (size, progress, how many
 * debts), Pressure asks a narrower question: "if something went wrong
 * this month, how much room do you actually have." A person could
 * have excellent long-term debt health (small, well-progressed debt)
 * but still be under real short-term pressure if liquidity is thin
 * relative to this month's obligations — Pressure is what catches that
 * case Health alone wouldn't.
 *
 * Two factors, unlike Health's five:
 * - Monthly obligation ratio: this month's required debt payments
 *   against monthly income.
 * - Liquidity buffer: how many months of obligations the current
 *   liquid balance could cover if income stopped entirely.
 *
 * Higher score means *more* pressure (inverted from [DebtHealthScore],
 * where higher is better) — named and read that way deliberately,
 * since "pressure" is a burden metric, not a health metric.
 */
fun calculateDebtPressure(debts: List<Debt>, monthlyIncome: Money, totalLiquidity: Money): DebtPressureScore {
    val owed = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
    val monthlyObligations = Money.sum(owed.mapNotNull { it.minimumMonthlyPayment })

    if (monthlyObligations.minorUnits == 0L) {
        return DebtPressureScore(score = 0, label = DebtPressureLabel.LOW)
    }

    // Obligation ratio (0-60): obligations at or above income itself is maximum pressure.
    val obligationComponent = if (monthlyIncome.minorUnits > 0) {
        val ratio = monthlyObligations.minorUnits.toDouble() / monthlyIncome.minorUnits
        (60 * ratio.coerceIn(0.0, 1.0)).roundToInt()
    } else 60 // No income at all against real obligations is maximum pressure, not unknown.

    // Liquidity buffer (0-40): less than one month of obligations covered is maximum pressure; six months or more is treated as a comfortable floor.
    val bufferMonths = totalLiquidity.minorUnits.toDouble() / monthlyObligations.minorUnits
    val bufferComponent = (40 * (1 - (bufferMonths / 6.0).coerceIn(0.0, 1.0))).roundToInt()

    val score = (obligationComponent + bufferComponent).coerceIn(0, 100)
    return DebtPressureScore(score = score, label = labelFor(score))
}

private fun labelFor(score: Int): DebtPressureLabel = when {
    score < 30 -> DebtPressureLabel.LOW
    score < 55 -> DebtPressureLabel.MODERATE
    score < 80 -> DebtPressureLabel.HIGH
    else -> DebtPressureLabel.EXTREME
}
