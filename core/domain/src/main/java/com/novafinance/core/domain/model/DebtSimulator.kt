package com.novafinance.core.domain.model

import java.time.LocalDate
import java.util.UUID

/**
 * "Increase payments, delay payments, add new debt, remove debt" —
 * 10.7's own list, modeled as a closed `sealed class` so
 * `simulateDebtScenario`'s `when` over adjustments is exhaustive and a
 * fifth adjustment type can't be added without the compiler flagging
 * every place that needs to handle it.
 */
sealed class DebtScenarioAdjustment {
    data class IncreasePayment(val debtId: String, val newMonthlyPayment: Money) : DebtScenarioAdjustment()
    data class DelayPayment(val debtId: String, val delayMonths: Int) : DebtScenarioAdjustment()
    data class AddDebt(val debt: Debt) : DebtScenarioAdjustment()
    data class RemoveDebt(val debtId: String) : DebtScenarioAdjustment()
}

data class DebtSimulationResult(
    val baseline: PayoffPlanResult,
    val scenario: PayoffPlanResult,
    /** Months earlier (positive) or later (negative) the scenario reaches debt freedom, vs. baseline. Null if either projection can't complete within the simulation horizon. */
    val freedomDateDeltaMonths: Int?,
    val goalImpacts: List<DebtDreamImpact>
)

/**
 * Pure function over a snapshot of debts — never reads or writes
 * [com.novafinance.core.domain.repository.DebtRepository]. "No data is
 * modified until confirmed" (10.7's own requirement) isn't enforced by
 * a flag or a confirmation step here; it's enforced structurally by
 * this function's signature never accepting a repository at all, so
 * there's no path through this code that could persist anything even
 * by mistake. A ViewModel calling this is free to discard the result,
 * and nothing anywhere else in the app would ever know the simulation
 * happened.
 */
fun simulateDebtScenario(
    currentDebts: List<Debt>,
    adjustments: List<DebtScenarioAdjustment>,
    goals: List<SavingsGoal> = emptyList(),
    strategy: PayoffStrategy = PayoffStrategy.FASTEST_FREEDOM,
    today: LocalDate = LocalDate.now()
): DebtSimulationResult {
    val baseline = simulatePayoffPlan(currentDebts, strategy, today = today)

    var workingDebts = currentDebts
    val delayMonths = mutableMapOf<String, Int>()

    for (adjustment in adjustments) {
        when (adjustment) {
            is DebtScenarioAdjustment.IncreasePayment -> {
                workingDebts = workingDebts.map { debt ->
                    if (debt.id == adjustment.debtId) debt.copy(minimumMonthlyPayment = adjustment.newMonthlyPayment) else debt
                }
            }
            is DebtScenarioAdjustment.DelayPayment -> {
                delayMonths[adjustment.debtId] = adjustment.delayMonths
            }
            is DebtScenarioAdjustment.AddDebt -> {
                workingDebts = workingDebts + adjustment.debt.copy(id = adjustment.debt.id.ifBlank { UUID.randomUUID().toString() })
            }
            is DebtScenarioAdjustment.RemoveDebt -> {
                workingDebts = workingDebts.filterNot { it.id == adjustment.debtId }
            }
        }
    }

    val scenario = simulatePayoffPlan(workingDebts, strategy, paymentDelayMonths = delayMonths, today = today)

    val delta = if (baseline.monthsToFreedom != null && scenario.monthsToFreedom != null) {
        baseline.monthsToFreedom - scenario.monthsToFreedom
    } else null

    // Freed-up (or newly committed) monthly cash flow, for the Debt vs
    // Dream Impact view (10.8) — the simplest honest read of "what
    // changed" is the difference in total minimum monthly payments
    // between the two scenarios; a debt that finishes earlier in the
    // scenario frees its payment up sooner, which this doesn't model
    // month-by-month, but the steady-state difference is a reasonable
    // single number to show goal impact against.
    val baselineMonthly = Money.sum(currentDebts.filter { it.direction == DebtDirection.I_OWE && it.isActive }.mapNotNull { it.minimumMonthlyPayment })
    val scenarioMonthly = Money.sum(workingDebts.filter { it.direction == DebtDirection.I_OWE && it.isActive }.mapNotNull { it.minimumMonthlyPayment })
    val cashFlowDelta = baselineMonthly - scenarioMonthly

    val goalImpacts = goals.filterNot { it.isComplete }.map { calculateDebtToGoalImpact(it, cashFlowDelta, today) }

    return DebtSimulationResult(
        baseline = baseline,
        scenario = scenario,
        freedomDateDeltaMonths = delta,
        goalImpacts = goalImpacts
    )
}
