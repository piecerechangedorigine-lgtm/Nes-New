package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.novafinance.core.data.entity.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAt ASC")
    fun observeGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity)

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("UPDATE goals SET currentAmountMinorUnits = currentAmountMinorUnits + :deltaMinorUnits WHERE id = :goalId")
    suspend fun contribute(goalId: String, deltaMinorUnits: Long)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun delete(goalId: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAll()
}
