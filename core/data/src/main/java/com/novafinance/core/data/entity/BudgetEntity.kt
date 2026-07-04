package com.novafinance.core.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.TransactionCategory
import java.time.YearMonth

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["month", "category"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: TransactionCategory,
    val monthlyLimitMinorUnits: Long,
    /** ISO-8601 "yyyy-MM" — one row per category per calendar month. */
    val month: String
)

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    category = category,
    monthlyLimit = Money(monthlyLimitMinorUnits),
    month = YearMonth.parse(month)
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    category = category,
    monthlyLimitMinorUnits = monthlyLimit.minorUnits,
    month = month.toString()
)
