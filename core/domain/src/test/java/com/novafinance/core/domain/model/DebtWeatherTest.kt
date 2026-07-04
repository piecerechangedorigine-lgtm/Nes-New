package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DebtWeatherTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun debt(dueDate: LocalDate? = null, percentPaid: Float = 0f) = Debt(
        id = "debt-1",
        name = "Test debt",
        direction = DebtDirection.I_OWE,
        type = DebtType.PERSONAL_LOAN,
        originalAmount = Money.fromMajor(1000.0),
        currentBalance = Money.fromMajor(1000.0 * (1 - percentPaid)),
        dueDate = dueDate,
        createdAt = today
    )

    @Test
    fun `weather maps health score bands correctly`() {
        assertThat(weatherFor(DebtHealthScore(90, DebtHealthLabel.HEALTHY, ""))).isEqualTo(DebtWeatherState.SUNNY)
        assertThat(weatherFor(DebtHealthScore(70, DebtHealthLabel.MODERATE, ""))).isEqualTo(DebtWeatherState.PARTLY_CLOUDY)
        assertThat(weatherFor(DebtHealthScore(50, DebtHealthLabel.MODERATE, ""))).isEqualTo(DebtWeatherState.CLOUDY)
        assertThat(weatherFor(DebtHealthScore(30, DebtHealthLabel.HIGH_RISK, ""))).isEqualTo(DebtWeatherState.RAINY)
        assertThat(weatherFor(DebtHealthScore(10, DebtHealthLabel.CRITICAL, ""))).isEqualTo(DebtWeatherState.STORM)
    }

    @Test
    fun `no active debt is a stable trend`() {
        assertThat(trendFor(emptyList(), today)).isEqualTo(DebtTrend.STABLE)
    }

    @Test
    fun `an overdue debt always reads as worsening regardless of progress`() {
        val overdue = listOf(debt(dueDate = today.minusDays(5), percentPaid = 0.8f))

        assertThat(trendFor(overdue, today)).isEqualTo(DebtTrend.WORSENING)
    }

    @Test
    fun `real progress with no overdue debts reads as improving`() {
        val progressing = listOf(debt(dueDate = today.plusMonths(6), percentPaid = 0.3f))

        assertThat(trendFor(progressing, today)).isEqualTo(DebtTrend.IMPROVING)
    }

    @Test
    fun `no progress and no overdue debts reads as stable`() {
        val fresh = listOf(debt(dueDate = today.plusMonths(6), percentPaid = 0f))

        assertThat(trendFor(fresh, today)).isEqualTo(DebtTrend.STABLE)
    }
}
