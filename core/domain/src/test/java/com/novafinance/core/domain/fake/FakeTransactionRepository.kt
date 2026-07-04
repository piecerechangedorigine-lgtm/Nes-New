package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.repository.TransactionRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Backed by a single in-memory list rather than a mocking framework —
 * these use cases are pure data transforms over whatever the repository
 * emits, so a real (if trivial) implementation of the interface exercises
 * the same Flow-combination code paths production wiring does, without
 * having to hand-configure return values for every call shape a mock
 * would need.
 */
class FakeTransactionRepository(initial: List<Transaction> = emptyList()) : TransactionRepository {

    private val state = MutableStateFlow(initial)

    fun setTransactions(transactions: List<Transaction>) {
        state.value = transactions
    }

    override fun observeTransactions(accountId: String?): Flow<List<Transaction>> =
        state.map { transactions -> transactions.filter { accountId == null || it.accountId == accountId } }

    override fun observeTransactionsForMonth(month: YearMonth, category: TransactionCategory?): Flow<List<Transaction>> =
        state.map { transactions ->
            transactions
                .filter { YearMonth.from(it.date) == month }
                .filter { category == null || it.category == category }
        }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        state.map { it.sortedByDescending { transaction -> transaction.date }.take(limit) }

    override suspend fun addTransaction(transaction: Transaction) {
        state.value = state.value + transaction
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        state.value = state.value.map { if (it.id == transaction.id) transaction else it }
    }

    override suspend fun deleteTransaction(transactionId: String) {
        state.value = state.value.filterNot { it.id == transactionId }
    }
}
