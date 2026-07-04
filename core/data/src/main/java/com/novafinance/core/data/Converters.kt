package com.novafinance.core.data

import androidx.room.TypeConverter
import com.novafinance.core.domain.model.BalanceUpdateMode
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.TransactionCategory

/**
 * Room has no automatic enum support — every enum column needs an explicit
 * round trip. Stored as the enum's [Enum.name] (not [Enum.ordinal]) so
 * reordering entries in any of these enums can never silently corrupt
 * existing rows.
 */
class Converters {
    @TypeConverter
    fun sourceTypeToString(value: FinancialSourceType): String = value.name

    @TypeConverter
    fun stringToSourceType(value: String): FinancialSourceType = FinancialSourceType.valueOf(value)

    @TypeConverter
    fun balanceUpdateModeToString(value: BalanceUpdateMode): String = value.name

    @TypeConverter
    fun stringToBalanceUpdateMode(value: String): BalanceUpdateMode = BalanceUpdateMode.valueOf(value)

    @TypeConverter
    fun categoryToString(value: TransactionCategory): String = value.name

    @TypeConverter
    fun stringToCategory(value: String): TransactionCategory = TransactionCategory.valueOf(value)

    @TypeConverter
    fun debtDirectionToString(value: DebtDirection): String = value.name

    @TypeConverter
    fun stringToDebtDirection(value: String): DebtDirection = DebtDirection.valueOf(value)

    @TypeConverter
    fun debtTypeToString(value: DebtType): String = value.name

    @TypeConverter
    fun stringToDebtType(value: String): DebtType = DebtType.valueOf(value)
}
