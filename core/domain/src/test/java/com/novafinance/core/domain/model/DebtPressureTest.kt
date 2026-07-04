package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DebtPressureTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun debt(payment: Double?) = Debt(
        id = "debt-$payment",
        name = "Test debt",
        direction = DebtDirection.I_OWE,
        type = DebtType.PERSONAL_LOAN,
        originalAmount = Money.fromMajor(5000.0),
        currentBalance = Money.fromMajor(5000.0),
        minimumMonthlyPayment = payment?.let { Money.fromMajor(it) },
        createdAt = today
    )

    @Test
    fun `no monthly obligations at all is always low pressure`() {
        val pressure = calculateDebtPressure(listOf(debt(null)), Money.fromMajor(3000.0), Money.fromMajor(5000.0))

        assertThat(pressure.score).isEqualTo(0)
        assertThat(pressure.label).isEqualTo(DebtPressureLabel.LOW)
    }

    @Test
    fun `obligations approaching all of income with thin liquidity is extreme pressure`() {
        val debts = listOf(debt(payment = 2800.0))

        val pressure = calculateDebtPressure(debts, monthlyIncome = Money.fromMajor(3000.0), totalLiquidity = Money.fromMajor(500.0))

        assertThat(pressure.label).isEqualTo(DebtPressureLabel.EXTREME)
    }

    @Test
    fun `small obligations against strong income and liquidity is low pressure`() {
        val debts = listOf(debt(payment = 100.0))

        val pressure = calculateDebtPressure(debts, monthlyIncome = Money.fromMajor(6000.0), totalLiquidity = Money.fromMajor(30000.0))

        assertThat(pressure.label).isEqualTo(DebtPressureLabel.LOW)
    }

    @Test
    fun `more liquidity buffer reduces pressure all else equal`() {
        val debts = listOf(debt(payment = 500.0))

        val thinBuffer = calculateDebtPressure(debts, Money.fromMajor(3000.0), Money.fromMajor(500.0))
        val healthyBuffer = calculateDebtPressure(debts, Money.fromMajor(3000.0), Money.fromMajor(10000.0))

        assertThat(healthyBuffer.score).isLessThan(thinBuffer.score)
    }

    @Test
    fun `score is always within 0 to 100`() {
        val debts = listOf(debt(payment = 100000.0))

        val pressure = calculateDebtPressure(debts, Money.ZERO, Money.ZERO)

        assertThat(pressure.score).isIn(0..100)
    }
}
