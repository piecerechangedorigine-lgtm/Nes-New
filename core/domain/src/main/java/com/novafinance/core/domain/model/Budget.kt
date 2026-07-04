package com.novafinance.core.domain.model

import java.time.YearMonth

/**
 * A budget is a spending cap per category per calendar month. It never
 * stores how much has actually been spent — that's derived at read time
 * from [com.novafinance.core.domain.repository.TransactionRepository] by
 * GetBudgetProgressUseCase, so a budget can never silently disagree with
 * the transaction ledger it's measuring.
 */
data class Budget(
    val id: String,
    val category: TransactionCategory,
    val monthlyLimit: Money,
    val month: YearMonth
)

/** Read-time aggregate: a [budget] joined with actual spend for its month. Never persisted directly. */
data class BudgetProgress(
    val budget: Budget,
    val spent: Money
) {
    val remaining: Money get() = budget.monthlyLimit - spent
    val percentUsed: Float get() = spent.ratioOf(budget.monthlyLimit)
    val isOverLimit: Boolean get() = spent > budget.monthlyLimit
}
