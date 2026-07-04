package com.novafinance.core.domain.model

import java.time.LocalDate
import java.time.Period

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Money,
    val currentAmount: Money,
    val targetDate: LocalDate?,
    val createdAt: LocalDate
) {
    val remaining: Money get() = (targetAmount - currentAmount).let { if (it.isNegative) Money.ZERO else it }
    val percentComplete: Float get() = currentAmount.ratioOf(targetAmount)
    val isComplete: Boolean get() = currentAmount >= targetAmount
}

/**
 * Read-time forecast for a goal with a target date: the flat monthly
 * contribution required to hit it on time, computed from whole calendar
 * months remaining. This is deliberately a simple linear projection, not
 * a trend model fit to past contribution history — that needs the
 * transaction-history analysis that ships with Analytics in Phase 5.
 */
data class GoalForecast(
    val goal: SavingsGoal,
    val requiredMonthlyContribution: Money?,
    val monthsRemaining: Int?
)

fun SavingsGoal.forecast(today: LocalDate = LocalDate.now()): GoalForecast {
    if (targetDate == null || isComplete) {
        return GoalForecast(goal = this, requiredMonthlyContribution = null, monthsRemaining = null)
    }
    val months = Period.between(today.withDayOfMonth(1), targetDate.withDayOfMonth(1)).toTotalMonths().toInt()
    val monthsRemaining = months.coerceAtLeast(1)
    val required = Money(remaining.minorUnits / monthsRemaining)
    return GoalForecast(goal = this, requiredMonthlyContribution = required, monthsRemaining = monthsRemaining)
}
