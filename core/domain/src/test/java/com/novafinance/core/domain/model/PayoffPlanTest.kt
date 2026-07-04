package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class PayoffPlanTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun debt(id: String, balance: Double, payment: Double?, interestRate: Double? = null) = Debt(
        id = id,
        name = id,
        direction = DebtDirection.I_OWE,
        type = DebtType.PERSONAL_LOAN,
        originalAmount = Money.fromMajor(balance),
        currentBalance = Money.fromMajor(balance),
        minimumMonthlyPayment = payment?.let { Money.fromMajor(it) },
        interestRatePercent = interestRate,
        createdAt = today
    )

    @Test
    fun `no active debt is immediately debt free`() {
        val result = simulatePayoffPlan(emptyList(), PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(result.monthsToFreedom).isEqualTo(0)
        assertThat(result.debtFreeDate).isEqualTo(today)
        assertThat(result.totalInterestPaid).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a debt with no interest and an evenly divisible payment pays off in exactly the expected number of months`() {
        // 1200 balance at 100 per month with zero interest = exactly 12 months.
        val debts = listOf(debt("d1", balance = 1200.0, payment = 100.0, interestRate = null))

        val result = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(result.monthsToFreedom).isEqualTo(12)
        assertThat(result.debtFreeDate).isEqualTo(today.plusMonths(12))
        assertThat(result.totalInterestPaid).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a debt with no payment and no interest never reaches freedom`() {
        val stuck = listOf(debt("d1", balance = 500.0, payment = null))

        val result = simulatePayoffPlan(stuck, PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(result.monthsToFreedom).isNull()
        assertThat(result.debtFreeDate).isNull()
    }

    @Test
    fun `fastest freedom orders smallest balance first`() {
        val debts = listOf(
            debt("big", balance = 5000.0, payment = 100.0),
            debt("small", balance = 500.0, payment = 50.0)
        )

        val result = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(result.payoffOrder.first()).isEqualTo("small")
    }

    @Test
    fun `lowest interest orders highest rate first`() {
        val debts = listOf(
            debt("low_rate", balance = 1000.0, payment = 50.0, interestRate = 5.0),
            debt("high_rate", balance = 1000.0, payment = 50.0, interestRate = 24.0)
        )

        val result = simulatePayoffPlan(debts, PayoffStrategy.LOWEST_INTEREST, today = today)

        assertThat(result.payoffOrder.first()).isEqualTo("high_rate")
    }

    @Test
    fun `avalanche pays no more total interest than snowball when rates diverge and extra budget exists`() {
        val debts = listOf(
            debt("high_rate_big", balance = 5000.0, payment = 100.0, interestRate = 24.0),
            debt("low_rate_small", balance = 1000.0, payment = 50.0, interestRate = 5.0)
        )

        val snowball = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, extraMonthlyBudget = Money.fromMajor(200.0), today = today)
        val avalanche = simulatePayoffPlan(debts, PayoffStrategy.LOWEST_INTEREST, extraMonthlyBudget = Money.fromMajor(200.0), today = today)

        assertThat(avalanche.totalInterestPaid.minorUnits).isAtMost(snowball.totalInterestPaid.minorUnits)
    }

    @Test
    fun `extra monthly budget accelerates payoff versus minimum payments alone`() {
        val debts = listOf(debt("d1", balance = 3000.0, payment = 100.0))

        val minimumOnly = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, today = today)
        val withExtra = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, extraMonthlyBudget = Money.fromMajor(200.0), today = today)

        assertThat(withExtra.monthsToFreedom!!).isLessThan(minimumOnly.monthsToFreedom!!)
    }

    @Test
    fun `delaying payment on a debt increases total interest paid`() {
        val debts = listOf(debt("d1", balance = 2000.0, payment = 100.0, interestRate = 18.0))

        val noDelay = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, today = today)
        val delayed = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, paymentDelayMonths = mapOf("d1" to 6), today = today)

        assertThat(delayed.totalInterestPaid.minorUnits).isGreaterThan(noDelay.totalInterestPaid.minorUnits)
    }

    @Test
    fun `custom order is honored and unlisted debts fall back to the end`() {
        val debts = listOf(
            debt("a", balance = 1000.0, payment = 50.0),
            debt("b", balance = 1000.0, payment = 50.0),
            debt("c", balance = 1000.0, payment = 50.0)
        )

        val result = simulatePayoffPlan(debts, PayoffStrategy.CUSTOM, customOrder = listOf("c", "a"), today = today)

        assertThat(result.payoffOrder).isEqualTo(listOf("c", "a", "b"))
    }

    @Test
    fun `calculateDebtFreedomDate matches the zero extra budget baseline`() {
        val debts = listOf(debt("d1", balance = 1000.0, payment = 100.0))

        val freedom = calculateDebtFreedomDate(debts, today)
        val baseline = simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(freedom.monthsToFreedom).isEqualTo(baseline.monthsToFreedom)
    }

    @Test
    fun `receivables and inactive debts are excluded from the simulation entirely`() {
        val mixed = listOf(
            debt("owed", balance = 1000.0, payment = 100.0),
            debt("receivable", balance = 500.0, payment = 50.0).copy(direction = DebtDirection.OWED_TO_ME),
            debt("paid_off", balance = 0.0, payment = 50.0)
        )

        val result = simulatePayoffPlan(mixed, PayoffStrategy.FASTEST_FREEDOM, today = today)

        assertThat(result.payoffOrder).containsExactly("owed")
    }
}
