package com.novafinance.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.Money
import java.time.LocalDate

@Entity(
    tableName = "debts",
    indices = [Index("direction"), Index("isActive")]
)
data class DebtEntity(
    @PrimaryKey val id: String,
    val name: String,
    val direction: DebtDirection,
    val type: DebtType,
    val originalAmountMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val interestRatePercent: Double?,
    val minimumMonthlyPaymentMinorUnits: Long?,
    /** ISO-8601 "yyyy-MM-dd", nullable — 10.2's optional due date. */
    val dueDate: String?,
    val counterpartyName: String?,
    val notes: String?,
    val isActive: Boolean,
    /** ISO-8601 "yyyy-MM-dd". */
    val createdAt: String
)

fun DebtEntity.toDomain(): Debt = Debt(
    id = id,
    name = name,
    direction = direction,
    type = type,
    originalAmount = Money(originalAmountMinorUnits),
    currentBalance = Money(currentBalanceMinorUnits),
    interestRatePercent = interestRatePercent,
    minimumMonthlyPayment = minimumMonthlyPaymentMinorUnits?.let { Money(it) },
    dueDate = dueDate?.let { LocalDate.parse(it) },
    counterpartyName = counterpartyName,
    notes = notes,
    isActive = isActive,
    createdAt = LocalDate.parse(createdAt)
)

fun Debt.toEntity(): DebtEntity = DebtEntity(
    id = id,
    name = name,
    direction = direction,
    type = type,
    originalAmountMinorUnits = originalAmount.minorUnits,
    currentBalanceMinorUnits = currentBalance.minorUnits,
    interestRatePercent = interestRatePercent,
    minimumMonthlyPaymentMinorUnits = minimumMonthlyPayment?.minorUnits,
    dueDate = dueDate?.toString(),
    counterpartyName = counterpartyName,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt.toString()
)
