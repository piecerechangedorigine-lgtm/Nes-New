package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface TransactionRepository {
    fun observeTransactions(accountId: String? = null): Flow<List<Transaction>>
    fun observeTransactionsForMonth(month: YearMonth, category: TransactionCategory? = null): Flow<List<Transaction>>
    fun observeRecentTransactions(limit: Int): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
}
