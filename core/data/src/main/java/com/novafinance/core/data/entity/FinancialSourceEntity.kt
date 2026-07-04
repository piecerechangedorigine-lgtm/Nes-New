package com.novafinance.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.BalanceUpdateMode
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.Money
import java.time.Instant

/**
 * Table name "financial_sources" (renamed from "accounts" in
 * MIGRATION_2_3). [currentBalanceMinorUnits] deliberately keeps the
 * physical column name "balanceMinorUnits" from before that rename via
 * [ColumnInfo] rather than a SQL `RENAME COLUMN` — that statement isn't
 * supported on the SQLite version bundled with early Android 8.x
 * devices (minSdk 26), so every migration this table has had so far
 * only ever needs `ADD COLUMN` and `UPDATE`, both safe on every
 * supported OS version. Phase 11's `MIGRATION_4_5` follows the same rule.
 */
@Entity(tableName = "financial_sources")
data class FinancialSourceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: FinancialSourceType,
    @ColumnInfo(name = "balanceMinorUnits") val currentBalanceMinorUnits: Long,
    val availableBalanceMinorUnits: Long,
    val currency: String,
    val notes: String?,
    val isActive: Boolean,
    val balanceUpdateMode: BalanceUpdateMode,
    val creditLimitMinorUnits: Long?,
    val includeInLiquidity: Boolean,
    val includeInSpendingPower: Boolean,
    val includeInForecast: Boolean,
    val includeInGoals: Boolean,
    val includeInAnalytics: Boolean,
    val isEmergencyReserve: Boolean,
    val groupId: String?,
    /** References a Debt.id — Phase 11.5.1's reconciliation link. Null for every unlinked source (the common case). */
    val linkedDebtId: String?,
    val createdAtEpochMillis: Long
)

fun FinancialSourceEntity.toDomain(): FinancialSource = FinancialSource(
    id = id,
    name = name,
    type = type,
    currentBalance = Money(currentBalanceMinorUnits),
    availableBalance = Money(availableBalanceMinorUnits),
    currency = currency,
    notes = notes,
    isActive = isActive,
    balanceUpdateMode = balanceUpdateMode,
    creditLimit = creditLimitMinorUnits?.let { Money(it) },
    includeInLiquidity = includeInLiquidity,
    includeInSpendingPower = includeInSpendingPower,
    includeInForecast = includeInForecast,
    includeInGoals = includeInGoals,
    includeInAnalytics = includeInAnalytics,
    isEmergencyReserve = isEmergencyReserve,
    groupId = groupId,
    linkedDebtId = linkedDebtId,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

fun FinancialSource.toEntity(): FinancialSourceEntity = FinancialSourceEntity(
    id = id,
    name = name,
    type = type,
    currentBalanceMinorUnits = currentBalance.minorUnits,
    availableBalanceMinorUnits = availableBalance.minorUnits,
    currency = currency,
    notes = notes,
    isActive = isActive,
    balanceUpdateMode = balanceUpdateMode,
    creditLimitMinorUnits = creditLimit?.minorUnits,
    includeInLiquidity = includeInLiquidity,
    includeInSpendingPower = includeInSpendingPower,
    includeInForecast = includeInForecast,
    includeInGoals = includeInGoals,
    includeInAnalytics = includeInAnalytics,
    isEmergencyReserve = isEmergencyReserve,
    groupId = groupId,
    linkedDebtId = linkedDebtId,
    createdAtEpochMillis = createdAt.toEpochMilli()
)
