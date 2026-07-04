package com.novafinance.core.domain.model

import java.time.LocalDate
import kotlin.math.roundToLong

/**
 * "Pay smallest debts first" (classic debt snowball — the psychological
 * momentum of closing accounts quickly), "pay highest-cost debts first"
 * (classic debt avalanche — provably the least total interest paid),
 * a hybrid, or a person's own explicit order. AI (this engine) computes
 * every strategy's projected outcome; nothing here picks one *for* the
 * person or applies it automatically — see 10.6's own "AI provides
 * suggestions only" and the Freedom First principle this whole phase
 * follows.
 */
enum class PayoffStrategy { FASTEST_FREEDOM, LOWEST_INTEREST, BALANCED, CUSTOM }

data class PayoffPlanResult(
    val strategy: PayoffStrategy,
    /** Null if the debts as entered can never be paid off (payments too small to outpace interest) — see [MAX_SIMULATION_MONTHS]. */
    val debtFreeDate: LocalDate?,
    val monthsToFreedom: Int?,
    val totalInterestPaid: Money,
    /** Debt ids in the order this strategy pays them off, for display — "you'll clear these in this order." */
    val payoffOrder: List<String>
)

/** 50 years — long enough for any real mortgage, short enough that a debt that can't mathematically be paid off (payment doesn't cover accruing interest) terminates the simulation instead of looping forever. */
private const val MAX_SIMULATION_MONTHS = 600

/**
 * Real month-by-month amortization — interest accrues before the
 * month's payment is applied, matching how every real lender actually
 * calculates it — not a division shortcut. This is what makes
 * [PayoffStrategy.LOWEST_INTEREST] and [PayoffStrategy.FASTEST_FREEDOM]
 * produce genuinely different [PayoffPlanResult.totalInterestPaid]
 * figures rather than the same number in a different order.
 *
 * [extraMonthlyBudget] beyond minimum payments goes entirely to
 * whichever debt is first in the strategy's order; once that debt is
 * paid off, its own minimum payment *and* the extra budget both roll
 * onto the next debt in order — the actual mechanic that makes
 * snowball/avalanche methods accelerate over time, not just a
 * per-debt independent projection.
 *
 * [paymentDelayMonths] (debt id → number of leading months with no
 * payment applied, interest still accruing) exists specifically for
 * the Debt Simulator's (10.7) "delay payments" scenario — empty by
 * default, so 10.5's Debt Freedom Date and 10.6's payoff strategies
 * never pay this cost unless a simulation explicitly asks for it.
 */
fun simulatePayoffPlan(
    debts: List<Debt>,
    strategy: PayoffStrategy,
    extraMonthlyBudget: Money = Money.ZERO,
    customOrder: List<String> = emptyList(),
    paymentDelayMonths: Map<String, Int> = emptyMap(),
    today: LocalDate = LocalDate.now()
): PayoffPlanResult {
    val owed = debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff }
    val order = orderFor(owed, strategy, customOrder)

    if (owed.isEmpty()) {
        return PayoffPlanResult(strategy, debtFreeDate = today, monthsToFreedom = 0, totalInterestPaid = Money.ZERO, payoffOrder = emptyList())
    }

    // Working state: remaining balance per debt id, in priority order.
    val balances = order.associateWith { id -> owed.first { it.id == id }.currentBalance.minorUnits }.toMutableMap()
    val monthlyRates = order.associateWith { id ->
        val rate = owed.first { it.id == id }.interestRatePercent ?: 0.0
        rate / 100.0 / 12.0
    }
    val minimumPayments = order.associateWith { id -> owed.first { it.id == id }.minimumMonthlyPayment?.minorUnits ?: 0L }

    var totalInterest = 0L
    var month = 0

    while (balances.values.any { it > 0 } && month < MAX_SIMULATION_MONTHS) {
        month++
        var extraPool = extraMonthlyBudget.minorUnits

        for (id in order) {
            val balance = balances[id] ?: continue
            if (balance <= 0) continue

            val interest = (balance * monthlyRates.getValue(id)).roundToLong()
            totalInterest += interest
            var newBalance = balance + interest

            val delayMonths = paymentDelayMonths[id] ?: 0
            val minimum = if (month <= delayMonths) 0L else minimumPayments.getValue(id).coerceAtMost(newBalance)
            newBalance -= minimum

            balances[id] = newBalance
        }

        // Extra budget flows to the first unpaid debt in priority
        // order — this is what accelerates the plan over the minimum-
        // payments-only baseline, and what rolls a paid-off debt's own
        // minimum payment into the next one via the loop simply moving on.
        for (id in order) {
            if (extraPool <= 0) break
            val balance = balances[id] ?: continue
            if (balance <= 0) continue
            val applied = extraPool.coerceAtMost(balance)
            balances[id] = balance - applied
            extraPool -= applied
        }
    }

    val payoffComplete = balances.values.all { it <= 0 }
    return PayoffPlanResult(
        strategy = strategy,
        debtFreeDate = if (payoffComplete) today.plusMonths(month.toLong()) else null,
        monthsToFreedom = if (payoffComplete) month else null,
        totalInterestPaid = Money(totalInterest),
        payoffOrder = order
    )
}

/**
 * The baseline Debt Freedom Date (10.5) — minimum payments only, no
 * strategy preference, no extra budget. Every [PayoffStrategy]
 * projection in [simulatePayoffPlan] is a variation on this same
 * engine; this is just the "if nothing changes" case.
 */
fun calculateDebtFreedomDate(debts: List<Debt>, today: LocalDate = LocalDate.now()): PayoffPlanResult =
    simulatePayoffPlan(debts, PayoffStrategy.FASTEST_FREEDOM, extraMonthlyBudget = Money.ZERO, today = today)

private fun orderFor(owed: List<Debt>, strategy: PayoffStrategy, customOrder: List<String>): List<String> = when (strategy) {
    PayoffStrategy.FASTEST_FREEDOM -> owed.sortedBy { it.currentBalance.minorUnits }.map { it.id }
    PayoffStrategy.LOWEST_INTEREST -> owed.sortedByDescending { it.interestRatePercent ?: 0.0 }.map { it.id }
    PayoffStrategy.BALANCED -> {
        val byBalance = owed.sortedBy { it.currentBalance.minorUnits }.map { it.id }
        val byInterest = owed.sortedByDescending { it.interestRatePercent ?: 0.0 }.map { it.id }
        owed.map { it.id }
            .sortedBy { id -> byBalance.indexOf(id) + byInterest.indexOf(id) }
    }
    PayoffStrategy.CUSTOM -> {
        val known = customOrder.filter { id -> owed.any { it.id == id } }
        val missing = owed.map { it.id }.filterNot { it in known }
        known + missing
    }
}
