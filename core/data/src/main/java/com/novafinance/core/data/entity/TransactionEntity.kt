package com.novafinance.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    indices = [Index("accountId"), Index(value = ["date", "category"])],
    foreignKeys = [
        ForeignKey(
            entity = FinancialSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val merchant: String,
    val category: TransactionCategory,
    val amountMinorUnits: Long,
    /** ISO-8601 local date string (yyyy-MM-dd) — sorts and filters correctly as plain text, no converter round-trip needed for range queries. */
    val date: String,
    val note: String?
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    merchant = merchant,
    category = category,
    amount = Money(amountMinorUnits),
    date = LocalDate.parse(date),
    note = note
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    merchant = merchant,
    category = category,
    amountMinorUnits = amount.minorUnits,
    date = date.toString(),
    note = note
)
