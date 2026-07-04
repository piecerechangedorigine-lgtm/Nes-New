package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novafinance.core.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month")
    fun observeBudgets(month: String): Flow<List<BudgetEntity>>

    /** Every budget ever set, across every month — used by Backup only. BudgetRepository's domain interface stays deliberately month-scoped for everything else. */
    @Query("SELECT * FROM budgets")
    suspend fun getAll(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    suspend fun delete(budgetId: String)

    @Query("DELETE FROM budgets")
    suspend fun deleteAll()
}
