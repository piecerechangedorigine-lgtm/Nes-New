package com.novafinance.core.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

data class DebtDreamImpact(
    val goal: SavingsGoal,
    /** Positive means the goal finishes sooner, negative means later. Null when there isn't enough data to project either baseline or scenario (no historical saving rate yet, or the rate is zero). */
    val monthsDelta: Int?,
    val message: String
)

/**
 * "If this debt is repaid 6 months earlier, your car goal reaches
 * completion 4 months sooner" (10.8's own example) is fundamentally a
 * before/after comparison of *time to complete a goal at a given
 * monthly contribution rate*. Nova has no per-contribution timestamp
 * history to know a goal's real contribution rate (same limitation
 * [GoalHealth]'s pace component already documents), so the "baseline"
 * rate here is the same proxy [GoalHealth] uses: total saved so far
 * divided by months since the goal was created. This is a genuine
 * computed projection from real data, not a fabricated number — it's
 * just built on an averaged proxy rather than true contribution
 * history, and that's stated here rather than left implicit.
 */
fun calculateDebtToGoalImpact(goal: SavingsGoal, monthlyCashFlowDelta: Money, today: LocalDate = LocalDate.now()): DebtDreamImpact {
    if (goal.isComplete) {
        return DebtDreamImpact(goal, monthsDelta = 0, message = "\"${goal.name}\" is already complete.")
    }

    val monthsSinceCreated = ChronoUnit.MONTHS.between(goal.createdAt, today).coerceAtLeast(1)
    val amountSaved = if (goal.currentAmount.isNegative) Money.ZERO else goal.currentAmount
    val averageMonthlyRate = Money(amountSaved.minorUnits / monthsSinceCreated)

    val baselineMonths = monthsToComplete(goal.remaining, averageMonthlyRate)
    val adjustedRate = Money((averageMonthlyRate.minorUnits + monthlyCashFlowDelta.minorUnits).coerceAtLeast(0))
    val scenarioMonths = monthsToComplete(goal.remaining, adjustedRate)

    if (baselineMonths == null || scenarioMonths == null) {
        return DebtDreamImpact(
            goal = goal,
            monthsDelta = null,
            message = "\"${goal.name}\" doesn't have enough contribution history yet to project an impact."
        )
    }

    val delta = baselineMonths - scenarioMonths
    val message = when {
        delta > 0 -> "If this changes, \"${goal.name}\" could reach completion about $delta month${if (delta == 1) "" else "s"} sooner."
        delta < 0 -> "If this changes, \"${goal.name}\" could take about ${-delta} month${if (-delta == 1) "" else "s"} longer to reach completion."
        else -> "This wouldn't meaningfully change \"${goal.name}\"'s timeline."
    }

    return DebtDreamImpact(goal = goal, monthsDelta = delta, message = message)
}

private fun monthsToComplete(remaining: Money, monthlyRate: Money): Int? {
    if (remaining.minorUnits <= 0) return 0
    if (monthlyRate.minorUnits <= 0) return null
    return ceil(remaining.minorUnits.toDouble() / monthlyRate.minorUnits).roundToInt()
}
