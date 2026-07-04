package com.novafinance.core.domain.model

import kotlin.math.roundToInt

/**
 * Four factors, 100-point budget, every input already computed
 * elsewhere — [BalanceOverview] (11.3), [DebtHealthScore] (10.3), and
 * [ForecastSummary] (8.5.6). This is the one Phase 11 engine that
 * reaches across every other intelligence engine in the app rather
 * than computing something new from raw sources; it's a synthesis
 * score, not an independent one.
 *
 * - Liquidity (0-30): [BalanceOverview.totalLiquidity] against monthly
 *   expense — three months of expenses covered by liquid balance is
 *   treated as the comfortable ceiling.
 * - Emergency coverage (0-25): [BalanceOverview.emergencyReserve]
 *   against monthly expense — six months is the target, standard
 *   emergency-fund guidance, not a Nova-specific number.
 * - Debt burden (0-25): a direct scaling of [DebtHealthScore.score] —
 *   deliberately not a second, parallel debt calculation. If Debt
 *   Health says a person's debt position is fine, this factor agrees;
 *   there's no second opinion computed here.
 * - Forecast stability (0-20): [ForecastStatus] (surplus/on-track/
 *   deficit) scaled by [ForecastConfidence] — a projected deficit late
 *   in the month (high confidence) costs more than the same deficit
 *   projected on day two (low confidence, could easily turn around).
 */
fun calculateBalanceHealth(
    overview: BalanceOverview,
    monthlyExpense: Money,
    debtHealth: DebtHealthScore,
    forecast: ForecastSummary
): BalanceHealthScore {
    val liquidityComponent = bufferComponent(overview.totalLiquidity, monthlyExpense, targetMonths = 3.0, maxPoints = 30)
    val emergencyComponent = bufferComponent(overview.emergencyReserve, monthlyExpense, targetMonths = 6.0, maxPoints = 25)
    val debtBurdenComponent = (debtHealth.score * 25 / 100.0).roundToInt()
    val forecastComponent = forecastStabilityComponent(forecast)

    val score = (liquidityComponent + emergencyComponent + debtBurdenComponent + forecastComponent).coerceIn(0, 100)
    return BalanceHealthScore(score = score, label = labelFor(score))
}

private fun bufferComponent(balance: Money, monthlyExpense: Money, targetMonths: Double, maxPoints: Int): Int {
    if (monthlyExpense.minorUnits <= 0) return maxPoints / 2 // No expense data — neutral, not zero.
    val months = balance.minorUnits.toDouble() / monthlyExpense.minorUnits
    return (maxPoints * (months / targetMonths).coerceIn(0.0, 1.0)).roundToInt()
}

private fun forecastStabilityComponent(forecast: ForecastSummary): Int {
    val confidenceWeight = when (forecast.confidence) {
        ForecastConfidence.LOW -> 0.4
        ForecastConfidence.MEDIUM -> 0.7
        ForecastConfidence.HIGH -> 1.0
    }
    val statusPoints = when (forecast.status) {
        ForecastStatus.SURPLUS -> 20
        ForecastStatus.ON_TRACK -> 14
        ForecastStatus.DEFICIT -> 4
    }
    return (statusPoints * confidenceWeight).roundToInt()
}

private fun labelFor(score: Int): BalanceHealthLabel = when {
    score >= 80 -> BalanceHealthLabel.HEALTHY
    score >= 60 -> BalanceHealthLabel.STABLE
    score >= 35 -> BalanceHealthLabel.WARNING
    else -> BalanceHealthLabel.CRITICAL
}
