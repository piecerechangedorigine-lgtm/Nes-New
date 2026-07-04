package com.novafinance.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The current backup schema version this app writes and the highest
 * version it knows how to read. Bumped independently of
 * [com.novafinance.core.data.database.NovaDatabase]'s own Room schema
 * version — a backup is a portable snapshot with its own compatibility
 * contract, not a database dump.
 *
 * v2 (Phase 10) added [BackupPayload.debts] — a v1 backup (no debts
 * field) still parses fine via [kotlinx.serialization]'s default-value
 * handling (`debts` defaults to an empty list), so bumping this wasn't
 * strictly required for backward *reading* compatibility, but the
 * version number is bumped anyway since the schema's shape genuinely
 * changed and a person restoring an old v1 backup should be able to
 * tell, from the number alone, that it predates debt tracking.
 */
const val CURRENT_BACKUP_VERSION = 2

/**
 * A complete, portable snapshot of everything Nova stores locally.
 * Deliberately built from primitive-typed DTOs (String/Long/Boolean)
 * rather than serializing [FinancialSource]/[Transaction]/[Budget]/
 * [SavingsGoal]/[Debt] directly — those domain models are free to
 * change shape as the app evolves without that change silently
 * breaking the ability to read a backup file written by an older
 * version.
 */
@Serializable
data class BackupPayload(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val sources: List<BackupSourceDto>,
    val transactions: List<BackupTransactionDto>,
    val budgets: List<BackupBudgetDto>,
    val goals: List<BackupGoalDto>,
    val debts: List<BackupDebtDto> = emptyList()
)

@Serializable
data class BackupSourceDto(
    val id: String,
    val name: String,
    val type: String,
    val currentBalanceMinorUnits: Long,
    val availableBalanceMinorUnits: Long,
    val currency: String,
    val notes: String?,
    val isActive: Boolean,
    val balanceUpdateMode: String,
    val createdAtEpochMillis: Long
)

@Serializable
data class BackupTransactionDto(
    val id: String,
    val accountId: String,
    val merchant: String,
    val category: String,
    val amountMinorUnits: Long,
    val date: String,
    val note: String? = null
)

@Serializable
data class BackupBudgetDto(
    val id: String,
    val category: String,
    val monthlyLimitMinorUnits: Long,
    val month: String
)

@Serializable
data class BackupGoalDto(
    val id: String,
    val name: String,
    val targetAmountMinorUnits: Long,
    val currentAmountMinorUnits: Long,
    val targetDate: String?,
    val createdAt: String
)

@Serializable
data class BackupDebtDto(
    val id: String,
    val name: String,
    val direction: String,
    val type: String,
    val originalAmountMinorUnits: Long,
    val currentBalanceMinorUnits: Long,
    val interestRatePercent: Double? = null,
    val minimumMonthlyPaymentMinorUnits: Long? = null,
    val dueDate: String? = null,
    val counterpartyName: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val createdAt: String
)

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun BackupPayload.toJson(): String = backupJson.encodeToString(this)

/**
 * Thrown for anything wrong with a backup file — corrupt JSON, a
 * missing required field, or a [BackupPayload.schemaVersion] newer than
 * this app understands. A dedicated exception type rather than letting
 * [kotlinx.serialization.SerializationException] leak through directly,
 * so callers get one consistent failure to handle regardless of which
 * specific thing about the file was wrong.
 */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun parseBackupPayload(json: String): BackupPayload {
    val payload = try {
        backupJson.decodeFromString<BackupPayload>(json)
    } catch (e: Exception) {
        throw InvalidBackupException("This file doesn't look like a valid Nova backup.", e)
    }
    if (payload.schemaVersion > CURRENT_BACKUP_VERSION) {
        throw InvalidBackupException(
            "This backup was made with a newer version of Nova (backup v${payload.schemaVersion}, " +
                "this app supports up to v$CURRENT_BACKUP_VERSION). Update the app before restoring it."
        )
    }
    return payload
}
