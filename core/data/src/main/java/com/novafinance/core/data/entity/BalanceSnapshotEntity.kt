package com.novafinance.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.BalanceSnapshot
import com.novafinance.core.domain.model.Money
import java.time.Instant

@Entity(tableName = "balance_snapshots", indices = [Index("sourceId")])
data class BalanceSnapshotEntity(
    @PrimaryKey val id: String,
    val sourceId: String,
    val balanceMinorUnits: Long,
    val recordedAtEpochMillis: Long
)

fun BalanceSnapshotEntity.toDomain(): BalanceSnapshot = BalanceSnapshot(
    id = id,
    sourceId = sourceId,
    balance = Money(balanceMinorUnits),
    recordedAt = Instant.ofEpochMilli(recordedAtEpochMillis)
)

fun BalanceSnapshot.toEntity(): BalanceSnapshotEntity = BalanceSnapshotEntity(
    id = id,
    sourceId = sourceId,
    balanceMinorUnits = balance.minorUnits,
    recordedAtEpochMillis = recordedAt.toEpochMilli()
)
