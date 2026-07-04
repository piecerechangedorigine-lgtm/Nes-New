package com.novafinance.core.domain.model

data class BalanceSuggestion(val message: String, val priority: SuggestionPriority)

enum class SuggestionPriority { INFO, WARNING }

/**
 * "Suggestions never execute automatically" (11.10's own requirement,
 * and this whole phase's Freedom First principle) is enforced the same
 * structural way [simulateDebtScenario] enforces "no data modified
 * until confirmed": this function's signature has no repository to
 * write through. It returns `List<BalanceSuggestion>` — plain text and
 * a priority — never an action a caller could accidentally execute.
 *
 * Every suggestion here is a direct, checkable read of numbers this
 * app already computed elsewhere (credit utilization, emergency
 * coverage, goal funding pace, and — as of Phase 11.5.1 — reconciliation
 * conflicts) — never a fabricated recommendation with nothing behind
 * it, the same standard `AssistantInsightEngine` and the AI Debt Coach
 * hold every reply to.
 */
fun generateBalanceSuggestions(
    sources: List<FinancialSource>,
    overview: BalanceOverview,
    goalForecasts: List<GoalForecast>,
    monthlyExpense: Money,
    debts: List<Debt> = emptyList()
): List<BalanceSuggestion> {
    val suggestions = mutableListOf<BalanceSuggestion>()

    // Reconciliation conflicts first — acting on a suggestion below
    // that's computed from a source with a known-stale balance (see
    // effectiveLiabilityBalance) is worse than surfacing the conflict itself first.
    detectReconciliationConflicts(sources, debts).forEach { conflict ->
        suggestions += BalanceSuggestion(
            "\"${conflict.source.name}\" shows ${conflict.sourceBalance.formatted()} but its linked debt shows " +
                "${conflict.debtBalance.formatted()} — update one so your Balance Intelligence stays accurate.",
            SuggestionPriority.WARNING
        )
    }

    sources.filter { it.isActive }.forEach { source ->
        val utilization = effectiveCreditCardUtilization(source, debts) ?: source.creditCardUtilization
        if (utilization != null && utilization.label == CreditUtilizationLabel.CRITICAL) {
            suggestions += BalanceSuggestion(
                "\"${source.name}\" credit utilization is at ${utilization.utilizationPercent}% — over 80% can affect your credit standing.",
                SuggestionPriority.WARNING
            )
        } else if (utilization != null && utilization.label == CreditUtilizationLabel.HIGH_UTILIZATION) {
            suggestions += BalanceSuggestion(
                "\"${source.name}\" credit utilization exceeds 50% — worth paying down before it climbs further.",
                SuggestionPriority.INFO
            )
        }
    }

    if (monthlyExpense.minorUnits > 0) {
        val emergencyMonths = overview.emergencyReserve.minorUnits.toDouble() / monthlyExpense.minorUnits
        if (emergencyMonths < 1.0 && overview.availableSpendingPower.minorUnits > monthlyExpense.minorUnits) {
            // Only suggested when there's real spare spending power to
            // draw from — recommending a transfer from money that isn't
            // actually free to move would be exactly the kind of
            // unchecked suggestion this engine is meant to avoid.
            val suggestedAmount = Money(monthlyExpense.minorUnits) // one month's expense as a starting move, not the whole gap at once
            suggestions += BalanceSuggestion(
                "Your emergency reserve covers less than a month of expenses. Consider moving ${suggestedAmount.formatted()} from checking toward it.",
                SuggestionPriority.WARNING
            )
        }
    }

    goalForecasts.forEach { forecast ->
        val required = forecast.requiredMonthlyContribution ?: return@forEach
        val monthsRemaining = forecast.monthsRemaining ?: return@forEach
        if (monthsRemaining <= 2 && required.minorUnits > 0) {
            suggestions += BalanceSuggestion(
                "\"${forecast.goal.name}\" is underfunded for its target date — it needs about ${required.formatted()}/month with $monthsRemaining month${if (monthsRemaining == 1) "" else "s"} left.",
                SuggestionPriority.INFO
            )
        }
    }

    return suggestions
}
