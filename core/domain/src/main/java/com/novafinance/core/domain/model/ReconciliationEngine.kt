package com.novafinance.core.domain.model

/**
 * The "ownership rule" this whole file exists to enforce: **when a
 * [FinancialSource] is linked to a [Debt] via [FinancialSource.linkedDebtId],
 * the [Debt] is authoritative for the balance owed.** A credit card's
 * `FinancialSource.currentBalance` and its linked `Debt.currentBalance`
 * are two independently-editable fields describing the same real-world
 * obligation — nothing in Room enforces they agree, and before this
 * phase nothing in the domain layer even checked. Every calculation
 * that cares about a linked source's liability amount goes through
 * [effectiveLiabilityBalance] rather than reading
 * [FinancialSource.currentBalance] directly, so the two fields
 * disagreeing doesn't silently produce two different answers depending
 * on which screen you're looking at.
 *
 * Why the *Debt* wins rather than the *FinancialSource*: `Debt` carries
 * the richer payoff-plan metadata (interest rate, minimum payment, due
 * date) a person is more likely to be actively maintaining once they've
 * bothered to link the two records at all — the whole reason someone
 * links a card is usually that they're tracking its payoff seriously.
 */
fun effectiveLiabilityBalance(source: FinancialSource, debts: List<Debt>): Money {
    val linkedDebt = source.linkedDebtId?.let { id -> debts.find { it.id == id } }
    return linkedDebt?.currentBalance ?: source.currentBalance
}

/**
 * The reconciliation-aware form of [FinancialSource.creditCardUtilization]
 * — same computation, but fed the authoritative (possibly linked-debt)
 * balance via [effectiveLiabilityBalance] instead of always reading
 * [FinancialSource.currentBalance] directly. A separate top-level
 * function rather than replacing the property: [FinancialSource] itself
 * has no way to see the current debt list (it's a plain data class with
 * no repository access, correctly so), so the reconciled version can
 * only exist as a function that's handed both.
 */
fun effectiveCreditCardUtilization(source: FinancialSource, debts: List<Debt>): CreditCardUtilization? {
    val limit = source.creditLimit ?: return null
    if (source.type != FinancialSourceType.CREDIT_CARD || limit.minorUnits <= 0) return null
    val usedAmount = effectiveLiabilityBalance(source, debts)
    return calculateCreditCardUtilization(usedAmount = usedAmount, creditLimit = limit)
}

/** One linked pair whose balances disagree — [source] and [debt] both describe the same real-world obligation but were last edited independently. */
data class ReconciliationConflict(
    val source: FinancialSource,
    val debt: Debt,
    val sourceBalance: Money,
    val debtBalance: Money
) {
    val difference: Money get() = sourceBalance - debtBalance
}

/**
 * "Prevent conflicting balances" (11.5.1's own requirement) is
 * satisfied two ways: [effectiveLiabilityBalance] makes sure every
 * *calculation* uses one consistent number regardless of the
 * disagreement, and this function surfaces the disagreement itself so
 * the person can see and fix it — reconciliation shouldn't just paper
 * over a real data-entry drift silently forever.
 *
 * A one-cent difference from independent rounding isn't worth
 * surfacing as a conflict; only a real, meaningful drift is.
 */
fun detectReconciliationConflicts(sources: List<FinancialSource>, debts: List<Debt>, toleranceMinorUnits: Long = 100): List<ReconciliationConflict> {
    val debtsById = debts.associateBy { it.id }
    return sources.mapNotNull { source ->
        val linkedDebt = source.linkedDebtId?.let { debtsById[it] } ?: return@mapNotNull null
        val diff = source.currentBalance.minorUnits - linkedDebt.currentBalance.minorUnits
        if (kotlin.math.abs(diff) <= toleranceMinorUnits) return@mapNotNull null
        ReconciliationConflict(
            source = source,
            debt = linkedDebt,
            sourceBalance = source.currentBalance,
            debtBalance = linkedDebt.currentBalance
        )
    }
}
