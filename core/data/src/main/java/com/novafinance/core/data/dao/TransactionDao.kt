package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novafinance.core.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        """
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId)
        ORDER BY date DESC
        """
    )
    fun observeTransactions(accountId: String?): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions
        WHERE date >= :monthStart AND date < :monthEndExclusive
        AND (:category IS NULL OR category = :category)
        ORDER BY date DESC
        """
    )
    fun observeTransactionsForMonth(monthStart: String, monthEndExclusive: String, category: String?): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun delete(transactionId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
