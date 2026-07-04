package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class DebtDreamImpactTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun goal(current: Double, target: Double, createdMonthsAgo: Long) = SavingsGoal(
        id = "goal-1",
        name = "Car fund",
        targetAmount = Money.fromMajor(target),
        currentAmount = Money.fromMajor(current),
        targetDate = null,
        createdAt = today.minusMonths(createdMonthsAgo)
    )

    @Test
    fun `a completed goal always reports zero delta`() {
        val impact = calculateDebtToGoalImpact(goal(1000.0, 1000.0, 6), Money.fromMajor(200.0), today)

        assertThat(impact.monthsDelta).isEqualTo(0)
    }

    @Test
    fun `freeing up monthly cash flow shortens the projected completion time`() {
        // Saved 600 over 6 months = 100/month average. Remaining 2400
        // at 100/month = 24 months baseline.
        val goalUnderTest = goal(current = 600.0, target = 3000.0, createdMonthsAgo = 6)

        val impact = calculateDebtToGoalImpact(goalUnderTest, monthlyCashFlowDelta = Money.fromMajor(100.0), today = today)

        assertThat(impact.monthsDelta).isNotNull()
        assertThat(impact.monthsDelta!!).isGreaterThan(0)
        assertThat(impact.message).contains("sooner")
    }

    @Test
    fun `losing monthly cash flow lengthens the projected completion time`() {
        val goalUnderTest = goal(current = 600.0, target = 3000.0, createdMonthsAgo = 6)

        val impact = calculateDebtToGoalImpact(goalUnderTest, monthlyCashFlowDelta = Money.fromMajor(-50.0), today = today)

        assertThat(impact.monthsDelta).isNotNull()
        assertThat(impact.monthsDelta!!).isLessThan(0)
        assertThat(impact.message).contains("longer")
    }

    @Test
    fun `zero historical saving rate and no cash flow change reports no usable projection`() {
        val stalled = goal(current = 0.0, target = 1000.0, createdMonthsAgo = 3)

        val impact = calculateDebtToGoalImpact(stalled, monthlyCashFlowDelta = Money.ZERO, today = today)

        assertThat(impact.monthsDelta).isNull()
        assertThat(impact.message).contains("enough contribution history")
    }
}
