package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.TransactionDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun observeTransactions(accountId: String?): Flow<List<Transaction>> =
        dao.observeTransactions(accountId).map { list -> list.map { it.toDomain() } }

    override fun observeTransactionsForMonth(month: YearMonth, category: TransactionCategory?): Flow<List<Transaction>> {
        val start = month.atDay(1).toString()
        val endExclusive = month.plusMonths(1).atDay(1).toString()
        return dao.observeTransactionsForMonth(start, endExclusive, category?.name)
            .map { list -> list.map { it.toDomain() } }
    }

    override fun observeRecentTransactions(limit: Int): Flow<List<Transaction>> =
        dao.observeRecentTransactions(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun addTransaction(transaction: Transaction) = dao.insert(transaction.toEntity())

    override suspend fun updateTransaction(transaction: Transaction) = dao.update(transaction.toEntity())

    override suspend fun deleteTransaction(transactionId: String) = dao.delete(transactionId)
}
