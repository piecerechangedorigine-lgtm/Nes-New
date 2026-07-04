package com.novafinance.core.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import java.time.LocalDate

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val name: String,
    val targetAmountMinorUnits: Long,
    val currentAmountMinorUnits: Long,
    val targetDate: String?,
    val createdAt: String
)

fun GoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    name = name,
    targetAmount = Money(targetAmountMinorUnits),
    currentAmount = Money(currentAmountMinorUnits),
    targetDate = targetDate?.let { LocalDate.parse(it) },
    createdAt = LocalDate.parse(createdAt)
)

fun SavingsGoal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    name = name,
    targetAmountMinorUnits = targetAmount.minorUnits,
    currentAmountMinorUnits = currentAmount.minorUnits,
    targetDate = targetDate?.toString(),
    createdAt = createdAt.toString()
)
