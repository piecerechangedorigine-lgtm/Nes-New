package com.novafinance.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.novafinance.core.data.entity.BalanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceSnapshotDao {
    @Query("SELECT * FROM balance_snapshots WHERE sourceId = :sourceId ORDER BY recordedAtEpochMillis ASC")
    fun observeSnapshots(sourceId: String): Flow<List<BalanceSnapshotEntity>>

    @Query("SELECT * FROM balance_snapshots ORDER BY recordedAtEpochMillis ASC")
    fun observeAllSnapshots(): Flow<List<BalanceSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: BalanceSnapshotEntity)

    @Query("DELETE FROM balance_snapshots")
    suspend fun deleteAll()
}
