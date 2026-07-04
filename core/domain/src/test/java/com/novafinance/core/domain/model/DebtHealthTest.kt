package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DebtHealthTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun debt(
        balance: Double,
        original: Double = balance,
        monthlyPayment: Double? = null,
        direction: DebtDirection = DebtDirection.I_OWE
    ) = Debt(
        id = "debt-$balance-$direction",
        name = "Test debt",
        direction = direction,
        type = DebtType.PERSONAL_LOAN,
        originalAmount = Money.fromMajor(original),
        currentBalance = Money.fromMajor(balance),
        minimumMonthlyPayment = monthlyPayment?.let { Money.fromMajor(it) },
        createdAt = today
    )

    @Test
    fun `no active owed debt is always a perfect score`() {
        val health = calculateDebtHealth(emptyList(), Money.fromMajor(3000.0), Money.fromMajor(5000.0))

        assertThat(health.score).isEqualTo(100)
        assertThat(health.label).isEqualTo(DebtHealthLabel.HEALTHY)
    }

    @Test
    fun `receivables never count against health`() {
        val onlyReceivable = listOf(debt(balance = 50000.0, direction = DebtDirection.OWED_TO_ME))

        val health = calculateDebtHealth(onlyReceivable, Money.fromMajor(3000.0), Money.fromMajor(5000.0))

        assertThat(health.score).isEqualTo(100)
    }

    @Test
    fun `a paid off debt does not count as active`() {
        val paidOff = listOf(debt(balance = 0.0, original = 1000.0))

        val health = calculateDebtHealth(paidOff, Money.fromMajor(3000.0), Money.fromMajor(5000.0))

        assertThat(health.score).isEqualTo(100)
    }

    @Test
    fun `small well progressed debt with ample income and liquidity scores high`() {
        val debts = listOf(debt(balance = 200.0, original = 2000.0, monthlyPayment = 100.0))

        val health = calculateDebtHealth(debts, monthlyIncome = Money.fromMajor(5000.0), totalLiquidity = Money.fromMajor(20000.0))

        assertThat(health.label).isAnyOf(DebtHealthLabel.HEALTHY, DebtHealthLabel.MODERATE)
    }

    @Test
    fun `large fresh debt eating most of income and exceeding liquidity scores poorly`() {
        val debts = listOf(
            debt(balance = 40000.0, original = 40000.0, monthlyPayment = 1800.0),
            debt(balance = 15000.0, original = 15000.0, monthlyPayment = 600.0)
        )

        val health = calculateDebtHealth(debts, monthlyIncome = Money.fromMajor(2500.0), totalLiquidity = Money.fromMajor(1000.0))

        assertThat(health.label).isAnyOf(DebtHealthLabel.HIGH_RISK, DebtHealthLabel.CRITICAL)
    }

    @Test
    fun `more open debts scores lower than fewer, all else equal`() {
        val oneDebt = listOf(debt(balance = 1000.0, monthlyPayment = 100.0))
        val fiveDebts = (1..5).map { debt(balance = 1000.0 + it, monthlyPayment = 100.0) }

        val healthOne = calculateDebtHealth(oneDebt, Money.fromMajor(3000.0), Money.fromMajor(10000.0))
        val healthFive = calculateDebtHealth(fiveDebts, Money.fromMajor(3000.0), Money.fromMajor(10000.0))

        assertThat(healthFive.score).isLessThan(healthOne.score)
    }

    @Test
    fun `missing income data does not crash and produces a neutral rather than zero score`() {
        val debts = listOf(debt(balance = 1000.0, monthlyPayment = 100.0))

        val health = calculateDebtHealth(debts, monthlyIncome = Money.ZERO, totalLiquidity = Money.fromMajor(5000.0))

        assertThat(health.score).isIn(0..100)
    }

    @Test
    fun `score is always within 0 to 100`() {
        val extreme = listOf(debt(balance = 10_000_000.0, original = 10_000_000.0, monthlyPayment = 500000.0))

        val health = calculateDebtHealth(extreme, Money.ZERO, Money.ZERO)

        assertThat(health.score).isIn(0..100)
    }
}
