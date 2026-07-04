package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novafinance.core.data.entity.DebtEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY createdAt ASC")
    fun observeDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun observeDebt(debtId: String): Flow<DebtEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(debt: DebtEntity)

    @Update
    suspend fun update(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :debtId")
    suspend fun delete(debtId: String)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()
}
