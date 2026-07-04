package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novafinance.core.data.entity.FinancialSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialSourceDao {
    @Query("SELECT * FROM financial_sources ORDER BY createdAtEpochMillis ASC")
    fun observeSources(): Flow<List<FinancialSourceEntity>>

    @Query("SELECT * FROM financial_sources WHERE id = :sourceId")
    fun observeSource(sourceId: String): Flow<FinancialSourceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(source: FinancialSourceEntity)

    @Update
    suspend fun update(source: FinancialSourceEntity)

    @Query("DELETE FROM financial_sources WHERE id = :sourceId")
    suspend fun delete(sourceId: String)

    @Query("DELETE FROM financial_sources")
    suspend fun deleteAll()
}
