package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class SavingsGoalForecastTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun goal(
        target: Double,
        current: Double,
        targetDate: LocalDate? = null
    ) = SavingsGoal(
        id = "goal-1",
        name = "Wedding fund",
        targetAmount = Money.fromMajor(target),
        currentAmount = Money.fromMajor(current),
        targetDate = targetDate,
        createdAt = today
    )

    @Test
    fun `goal with no target date has no required contribution or months remaining`() {
        val forecast = goal(target = 5000.0, current = 1000.0).forecast(today)

        assertThat(forecast.requiredMonthlyContribution).isNull()
        assertThat(forecast.monthsRemaining).isNull()
    }

    @Test
    fun `completed goal has no required contribution even with a target date`() {
        val forecast = goal(target = 1000.0, current = 1000.0, targetDate = today.plusMonths(6)).forecast(today)

        assertThat(forecast.requiredMonthlyContribution).isNull()
        assertThat(forecast.monthsRemaining).isNull()
    }

    @Test
    fun `splits the remaining amount evenly across whole calendar months remaining`() {
        // 6000 remaining over 6 months = 1000 per month.
        val forecast = goal(
            target = 7000.0,
            current = 1000.0,
            targetDate = today.plusMonths(6)
        ).forecast(today)

        assertThat(forecast.monthsRemaining).isEqualTo(6)
        assertThat(forecast.requiredMonthlyContribution).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `a target date in the current month still requires at least one month of contribution`() {
        // Target date is later this same calendar month — Period.between
        // on the 1st-of-month-normalized dates yields zero whole months,
        // which the forecast coerces up to 1 rather than dividing by zero.
        val forecast = goal(
            target = 1000.0,
            current = 0.0,
            targetDate = today.plusDays(10)
        ).forecast(today)

        assertThat(forecast.monthsRemaining).isEqualTo(1)
        assertThat(forecast.requiredMonthlyContribution).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `percentComplete and remaining never go negative once a goal is overfunded`() {
        val goal = goal(target = 500.0, current = 600.0)

        assertThat(goal.isComplete).isTrue()
        assertThat(goal.remaining).isEqualTo(Money.ZERO)
        assertThat(goal.percentComplete).isEqualTo(1f)
    }
}
