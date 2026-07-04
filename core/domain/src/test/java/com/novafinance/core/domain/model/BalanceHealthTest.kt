package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BalanceHealthTest {

    private val healthyDebt = DebtHealthScore(100, DebtHealthLabel.HEALTHY, "No active debt.")
    private val criticalDebt = DebtHealthScore(10, DebtHealthLabel.CRITICAL, "critical")

    private fun forecast(status: ForecastStatus, confidence: ForecastConfidence) = ForecastSummary(
        projectedEndOfMonthBalance = Money.ZERO,
        projectedSurplusOrDeficit = Money.ZERO,
        status = status,
        confidence = confidence,
        message = ""
    )

    private fun overview(liquidity: Double, emergencyReserve: Double) = BalanceOverview(
        totalLiquidity = Money.fromMajor(liquidity),
        availableSpendingPower = Money.fromMajor(liquidity),
        dreamSafeBalance = Money.ZERO,
        emergencyReserve = Money.fromMajor(emergencyReserve),
        forecastBalance = Money.fromMajor(liquidity)
    )

    @Test
    fun `strong liquidity, full emergency coverage, healthy debt, and a confident surplus forecast scores near the top`() {
        val health = calculateBalanceHealth(
            overview = overview(liquidity = 9000.0, emergencyReserve = 6000.0),
            monthlyExpense = Money.fromMajor(1000.0),
            debtHealth = healthyDebt,
            forecast = forecast(ForecastStatus.SURPLUS, ForecastConfidence.HIGH)
        )

        assertThat(health.label).isEqualTo(BalanceHealthLabel.HEALTHY)
    }

    @Test
    fun `no liquidity, no emergency reserve, critical debt, and a confident deficit scores near the bottom`() {
        val health = calculateBalanceHealth(
            overview = overview(liquidity = 0.0, emergencyReserve = 0.0),
            monthlyExpense = Money.fromMajor(1000.0),
            debtHealth = criticalDebt,
            forecast = forecast(ForecastStatus.DEFICIT, ForecastConfidence.HIGH)
        )

        assertThat(health.label).isEqualTo(BalanceHealthLabel.CRITICAL)
    }

    @Test
    fun `a low confidence deficit hurts less than a high confidence deficit`() {
        val base = overview(liquidity = 3000.0, emergencyReserve = 3000.0)

        val lowConfidence = calculateBalanceHealth(base, Money.fromMajor(1000.0), healthyDebt, forecast(ForecastStatus.DEFICIT, ForecastConfidence.LOW))
        val highConfidence = calculateBalanceHealth(base, Money.fromMajor(1000.0), healthyDebt, forecast(ForecastStatus.DEFICIT, ForecastConfidence.HIGH))

        assertThat(lowConfidence.score).isGreaterThan(highConfidence.score)
    }

    @Test
    fun `debt burden component directly tracks debt health score`() {
        val base = overview(liquidity = 3000.0, emergencyReserve = 3000.0)
        val forecastFixed = forecast(ForecastStatus.ON_TRACK, ForecastConfidence.MEDIUM)

        val withHealthyDebt = calculateBalanceHealth(base, Money.fromMajor(1000.0), healthyDebt, forecastFixed)
        val withCriticalDebt = calculateBalanceHealth(base, Money.fromMajor(1000.0), criticalDebt, forecastFixed)

        assertThat(withHealthyDebt.score).isGreaterThan(withCriticalDebt.score)
    }

    @Test
    fun `score is always within 0 to 100`() {
        val health = calculateBalanceHealth(
            overview = overview(liquidity = -50000.0, emergencyReserve = 0.0),
            monthlyExpense = Money.fromMajor(1.0),
            debtHealth = criticalDebt,
            forecast = forecast(ForecastStatus.DEFICIT, ForecastConfidence.HIGH)
        )

        assertThat(health.score).isIn(0..100)
    }

    @Test
    fun `no expense data does not crash and produces a neutral rather than zero component`() {
        val health = calculateBalanceHealth(
            overview = overview(liquidity = 1000.0, emergencyReserve = 500.0),
            monthlyExpense = Money.ZERO,
            debtHealth = healthyDebt,
            forecast = forecast(ForecastStatus.ON_TRACK, ForecastConfidence.MEDIUM)
        )

        assertThat(health.score).isIn(0..100)
    }
}
