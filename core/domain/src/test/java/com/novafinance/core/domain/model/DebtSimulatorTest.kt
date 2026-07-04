package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DebtSimulatorTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun debt(id: String, balance: Double, payment: Double?) = Debt(
        id = id,
        name = id,
        direction = DebtDirection.I_OWE,
        type = DebtType.PERSONAL_LOAN,
        originalAmount = Money.fromMajor(balance),
        currentBalance = Money.fromMajor(balance),
        minimumMonthlyPayment = payment?.let { Money.fromMajor(it) },
        createdAt = today
    )

    @Test
    fun `increasing a payment reaches freedom sooner than baseline`() {
        val debts = listOf(debt("d1", balance = 3000.0, payment = 100.0))
        val adjustments = listOf(DebtScenarioAdjustment.IncreasePayment("d1", Money.fromMajor(300.0)))

        val result = simulateDebtScenario(debts, adjustments, today = today)

        assertThat(result.freedomDateDeltaMonths).isNotNull()
        assertThat(result.freedomDateDeltaMonths!!).isGreaterThan(0)
    }

    @Test
    fun `delaying a payment reaches freedom later than baseline`() {
        val debts = listOf(debt("d1", balance = 3000.0, payment = 200.0))
        val adjustments = listOf(DebtScenarioAdjustment.DelayPayment("d1", delayMonths = 4))

        val result = simulateDebtScenario(debts, adjustments, today = today)

        assertThat(result.freedomDateDeltaMonths).isNotNull()
        assertThat(result.freedomDateDeltaMonths!!).isLessThan(0)
    }

    @Test
    fun `removing a debt reaches overall freedom no later than baseline`() {
        val debts = listOf(
            debt("small", balance = 500.0, payment = 100.0),
            debt("big", balance = 5000.0, payment = 100.0)
        )
        val adjustments = listOf(DebtScenarioAdjustment.RemoveDebt("big"))

        val result = simulateDebtScenario(debts, adjustments, today = today)

        assertThat(result.freedomDateDeltaMonths).isNotNull()
        assertThat(result.freedomDateDeltaMonths!!).isAtLeast(0)
    }

    @Test
    fun `adding a new debt never modifies the original list the caller passed in`() {
        val original = listOf(debt("d1", balance = 1000.0, payment = 100.0))
        val newDebt = debt("new", balance = 500.0, payment = 50.0)
        val adjustments = listOf(DebtScenarioAdjustment.AddDebt(newDebt))

        simulateDebtScenario(original, adjustments, today = today)

        assertThat(original).hasSize(1)
    }

    @Test
    fun `goal impacts are computed for every incomplete goal passed in`() {
        val debts = listOf(debt("d1", balance = 3000.0, payment = 100.0))
        val goals = listOf(
            SavingsGoal("g1", "Car", Money.fromMajor(5000.0), Money.fromMajor(500.0), null, today.minusMonths(5)),
            SavingsGoal("g2", "Done goal", Money.fromMajor(100.0), Money.fromMajor(100.0), null, today.minusMonths(1))
        )
        val adjustments = listOf(DebtScenarioAdjustment.IncreasePayment("d1", Money.fromMajor(300.0)))

        val result = simulateDebtScenario(debts, adjustments, goals = goals, today = today)

        assertThat(result.goalImpacts.map { it.goal.id }).containsExactly("g1")
    }

    @Test
    fun `no adjustments at all reproduces the baseline exactly`() {
        val debts = listOf(debt("d1", balance = 2000.0, payment = 150.0))

        val result = simulateDebtScenario(debts, emptyList(), today = today)

        assertThat(result.freedomDateDeltaMonths).isEqualTo(0)
    }
}
