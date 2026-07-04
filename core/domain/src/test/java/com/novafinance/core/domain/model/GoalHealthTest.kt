package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class GoalHealthTest {

    private val today = LocalDate.of(2026, 1, 1)

    private fun goal(target: Double, current: Double, createdAt: LocalDate = today, targetDate: LocalDate? = null) = SavingsGoal(
        id = "goal-1",
        name = "Wedding fund",
        targetAmount = Money.fromMajor(target),
        currentAmount = Money.fromMajor(current),
        targetDate = targetDate,
        createdAt = createdAt
    )

    @Test
    fun `a completed goal always scores a flat 100 excellent`() {
        val health = goal(target = 1000.0, current = 1000.0, targetDate = today.plusMonths(3))
            .forecast(today)
            .health(today)

        assertThat(health.score).isEqualTo(100)
        assertThat(health.label).isEqualTo(GoalHealthLabel.EXCELLENT)
    }

    @Test
    fun `perfectly on pace with a target date scores well`() {
        // Goal created 6 months before target, exactly half the time has
        // elapsed and exactly half the money is saved — textbook on-pace.
        val createdAt = today.minusMonths(3)
        val targetDate = today.plusMonths(3)
        val health = goal(target = 1000.0, current = 500.0, createdAt = createdAt, targetDate = targetDate)
            .forecast(today)
            .health(today)

        assertThat(health.score).isAtLeast(60)
        assertThat(health.label).isAnyOf(GoalHealthLabel.GOOD, GoalHealthLabel.EXCELLENT)
    }

    @Test
    fun `far behind pace with little time left scores poorly and suggests catching up`() {
        // Goal created a year ago, due in one month, but almost nothing saved.
        val createdAt = today.minusMonths(11)
        val targetDate = today.plusMonths(1)
        val health = goal(target = 10000.0, current = 100.0, createdAt = createdAt, targetDate = targetDate)
            .forecast(today)
            .health(today)

        assertThat(health.label).isAnyOf(GoalHealthLabel.AT_RISK, GoalHealthLabel.CRITICAL)
        assertThat(health.suggestedActions).isNotEmpty()
    }

    @Test
    fun `no target date never penalizes pace or urgency`() {
        val health = goal(target = 1000.0, current = 100.0, targetDate = null)
            .forecast(today)
            .health(today)

        // With no deadline, only the progress component (10% of 40 = 4)
        // plus fully-neutral pace/forecast/time components (30+20+10=60)
        // should contribute — nothing about "no deadline" should read as risky.
        assertThat(health.score).isAtLeast(60)
    }

    @Test
    fun `score is always clamped within 0 to 100`() {
        val health = goal(target = 1_000_000.0, current = 0.0, createdAt = today.minusYears(5), targetDate = today.minusDays(1))
            .forecast(today)
            .health(today)

        assertThat(health.score).isIn(0..100)
    }
}
