package com.novafinance.core.domain.model

import kotlin.math.roundToInt

data class CreditCardUtilization(
    val usedAmount: Money,
    val creditLimit: Money,
    val availableCredit: Money,
    val utilizationPercent: Int,
    val label: CreditUtilizationLabel
)

/** Standard real-world utilization guidance thresholds — the same bands most credit scoring models use, not a Nova-specific invention. */
enum class CreditUtilizationLabel { HEALTHY, MODERATE, HIGH_UTILIZATION, CRITICAL }

/**
 * Pure calculation — [FinancialSource.creditCardUtilization] is the one
 * place this actually gets called from, so a screen never needs to
 * import this function directly, only read the property. Utilization
 * over 100% (a card charged past its limit) is a real, representable
 * state, not clamped away — [availableCredit] simply floors at zero
 * while [utilizationPercent] keeps climbing, since hiding "you're over
 * your limit" would be exactly the wrong thing to smooth over.
 */
fun calculateCreditCardUtilization(usedAmount: Money, creditLimit: Money): CreditCardUtilization {
    val availableCredit = (creditLimit - usedAmount).let { if (it.isNegative) Money.ZERO else it }
    val utilizationPercent = if (creditLimit.minorUnits > 0) {
        ((usedAmount.minorUnits.toDouble() / creditLimit.minorUnits) * 100).roundToInt()
    } else 0

    val label = when {
        utilizationPercent < 30 -> CreditUtilizationLabel.HEALTHY
        utilizationPercent < 50 -> CreditUtilizationLabel.MODERATE
        utilizationPercent < 80 -> CreditUtilizationLabel.HIGH_UTILIZATION
        else -> CreditUtilizationLabel.CRITICAL
    }

    return CreditCardUtilization(
        usedAmount = usedAmount,
        creditLimit = creditLimit,
        availableCredit = availableCredit,
        utilizationPercent = utilizationPercent,
        label = label
    )
}
