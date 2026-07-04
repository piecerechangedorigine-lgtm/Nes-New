package com.novafinance.core.domain.model

import java.time.LocalDate

/**
 * A tracked obligation — either money the person owes, or money owed
 * to them. Deliberately a separate entity from [FinancialSource], not
 * a replacement for its `CREDIT_CARD` type: a [FinancialSource]
 * answers "where does money sit and how much is there right now,"
 * which a credit card account still needs to answer for liquidity
 * purposes. [Debt] answers a different question — "what's owed, to
 * whom, on what timeline, at what cost" — that a bare account balance
 * has no room for (no direction, no due date, no interest rate, no
 * payoff strategy). A person might reasonably track a credit card as
 * both: a [FinancialSource] for its spendable balance, and a [Debt]
 * for its payoff plan. Nothing today reconciles the two automatically
 * if someone does that — see `DEBT_ARCHITECTURE.md` and
 * `TECHNICAL_DEBT.md`.
 */
data class Debt(
    val id: String,
    val name: String,
    val direction: DebtDirection,
    val type: DebtType,
    val originalAmount: Money,
    /** What's left. Zero or less means paid off / fully recovered. */
    val currentBalance: Money,
    /** Annual percentage rate, if known. Optional — a family loan often has none; a credit card almost always does. Drives the Lowest Interest payoff strategy (10.6) and the Debt Health interest-cost factor. */
    val interestRatePercent: Double? = null,
    /** What's actually paid/received each month today, if there's a regular arrangement. Optional — Debt Freedom Date and payoff simulations can't project a debt with no payment figure at all. */
    val minimumMonthlyPayment: Money? = null,
    /**
     * Optional per 10.2 — "target repayment date" when [direction] is
     * [DebtDirection.I_OWE], "expected recovery date" when
     * [DebtDirection.OWED_TO_ME]. One field, not two, since a debt is
     * never both directions at once; the UI layer picks the label.
     */
    val dueDate: LocalDate? = null,
    /** Who the money is owed to/from — a name, not a structured contact. "Sarah", "Chase", "the business". */
    val counterpartyName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: LocalDate
) {
    val isPaidOff: Boolean get() = currentBalance.minorUnits <= 0
    val amountPaidDown: Money get() = (originalAmount - currentBalance).let { if (it.isNegative) Money.ZERO else it }
    val percentPaidDown: Float get() = amountPaidDown.ratioOf(originalAmount)
}

/**
 * "Both directions must be supported equally" per the 10.1 brief —
 * enforced structurally by every debt-level calculation (health,
 * weather, freedom date, payoff plans) taking a direction-filtered
 * list rather than assuming one direction, not by convention alone.
 */
enum class DebtDirection { I_OWE, OWED_TO_ME }

/**
 * Deliberately not direction-locked — [FAMILY_OR_FRIEND] and
 * [BUSINESS] are common in both directions (money lent to family is
 * exactly as real as money borrowed from them), so [DebtType] never
 * restricts which [DebtDirection] it can pair with. The form UI
 * defaults to sensible suggestions per direction without enforcing them.
 */
enum class DebtType(val displayName: String) {
    PERSONAL_LOAN("Personal Loan"),
    CREDIT_CARD("Credit Card"),
    FAMILY_OR_FRIEND("Family or Friend"),
    MORTGAGE("Mortgage"),
    CAR_LOAN("Car Loan"),
    BUSINESS("Business"),
    OTHER("Other")
}
