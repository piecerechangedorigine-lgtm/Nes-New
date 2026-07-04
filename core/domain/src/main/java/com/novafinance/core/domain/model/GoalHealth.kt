package com.novafinance.core.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * A single 0-100 score summarizing how healthy a goal's progress looks
 * right now, plus plain-language next steps. Deliberately simple,
 * explainable component scoring — the same "deliberately simple
 * heuristics, not a model" approach used everywhere else scoring shows
 * up in this codebase (Dashboard's insight banner, the Assistant).
 */
data class GoalHealth(
    val score: Int,
    val label: GoalHealthLabel,
    val suggestedActions: List<String>
)

enum class GoalHealthLabel { EXCELLENT, GOOD, AT_RISK, CRITICAL }

/**
 * Scores four factors out of a 100-point budget:
 * - Progress (0-40): straight percentComplete.
 * - Pace (0-30): percentComplete relative to how much of the goal's
 *   total timeline has elapsed. This is a proxy for "contribution
 *   consistency" — Nova has no per-contribution timestamp history today
 *   (contributions only ever increment a running total, see
 *   GoalRepository.contribute), so there's no real event stream to
 *   measure actual consistency against. Documented as a known
 *   simplification in TECHNICAL_DEBT.md, not silently glossed over.
 * - Forecast confidence (0-20): more months remaining until the target
 *   date reads as more comfortable/flexible, fewer as more urgent — a
 *   proxy for confidence given this model has no income context to
 *   judge real feasibility against.
 * - Remaining time (0-10): straightforward — more runway is safer.
 *
 * A goal with no target date or that's already complete short-circuits
 * to a flat high score, since "pace" and "urgency" are meaningless
 * without a deadline to measure against.
 */
fun GoalForecast.health(today: LocalDate = LocalDate.now()): GoalHealth {
    if (goal.isComplete) {
        return GoalHealth(score = 100, label = GoalHealthLabel.EXCELLENT, suggestedActions = listOf("Goal reached — consider setting a new target."))
    }

    val progressComponent = (goal.percentComplete * 40).roundToInt()

    val targetDate = goal.targetDate
    val paceComponent: Int
    val forecastComponent: Int
    val remainingTimeComponent: Int

    if (targetDate == null) {
        // No deadline means no pace to fall behind on and no urgency —
        // score these components as fully neutral/comfortable rather
        // than penalizing a goal for simply not having a target date.
        paceComponent = if (goal.percentComplete > 0f) 30 else 15
        forecastComponent = 20
        remainingTimeComponent = 10
    } else {
        val totalDays = ChronoUnit.DAYS.between(goal.createdAt, targetDate).coerceAtLeast(1)
        val elapsedDays = ChronoUnit.DAYS.between(goal.createdAt, today).coerceIn(0, totalDays)
        val elapsedRatio = elapsedDays.toDouble() / totalDays

        paceComponent = if (elapsedRatio <= 0.0) {
            30
        } else {
            (((goal.percentComplete.toDouble() / elapsedRatio).coerceAtMost(1.0)) * 30).roundToInt()
        }

        val months = monthsRemaining ?: 0
        forecastComponent = (months * 4).coerceIn(0, 20)
        remainingTimeComponent = when {
            months >= 3 -> 10
            months == 2 -> 6
            months == 1 -> 3
            else -> 0
        }
    }

    val score = (progressComponent + paceComponent + forecastComponent + remainingTimeComponent).coerceIn(0, 100)
    val label = labelFor(score)
    val actions = suggestedActionsFor(
        progressComponent = progressComponent,
        paceComponent = paceComponent,
        remainingTimeComponent = remainingTimeComponent,
        hasTargetDate = targetDate != null
    )

    return GoalHealth(score = score, label = label, suggestedActions = actions)
}

private fun labelFor(score: Int): GoalHealthLabel = when {
    score >= 80 -> GoalHealthLabel.EXCELLENT
    score >= 60 -> GoalHealthLabel.GOOD
    score >= 35 -> GoalHealthLabel.AT_RISK
    else -> GoalHealthLabel.CRITICAL
}

private fun suggestedActionsFor(
    progressComponent: Int,
    paceComponent: Int,
    remainingTimeComponent: Int,
    hasTargetDate: Boolean
): List<String> {
    val actions = mutableListOf<String>()
    if (progressComponent < 10) actions += "Add a contribution to build momentum."
    if (hasTargetDate && paceComponent < 15) actions += "Increase your contribution to catch up to your target date."
    if (hasTargetDate && remainingTimeComponent <= 3) actions += "Your target date is close — review whether it's still realistic."
    if (actions.isEmpty()) actions += "You're on track — no action needed."
    return actions
}
