package com.novafinance.core.domain.model

import java.time.LocalDate

/**
 * [amount] is signed: negative for spend, positive for income/refunds/transfers-in.
 * This is what lets a single list power both "recent activity" and
 * "this month's spend" without a separate isExpense flag drifting out of
 * sync with the category or the sign.
 */
data class Transaction(
    val id: String,
    val accountId: String,
    val merchant: String,
    val category: TransactionCategory,
    val amount: Money,
    val date: LocalDate,
    val note: String? = null
)
