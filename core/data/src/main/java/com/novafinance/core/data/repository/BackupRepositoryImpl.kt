package com.novafinance.core.data.repository

import androidx.room.withTransaction
import com.novafinance.core.data.dao.BudgetDao
import com.novafinance.core.data.dao.DebtDao
import com.novafinance.core.data.dao.FinancialSourceDao
import com.novafinance.core.data.dao.GoalDao
import com.novafinance.core.data.dao.TransactionDao
import com.novafinance.core.data.database.NovaDatabase
import com.novafinance.core.data.entity.BudgetEntity
import com.novafinance.core.data.entity.DebtEntity
import com.novafinance.core.data.entity.FinancialSourceEntity
import com.novafinance.core.data.entity.GoalEntity
import com.novafinance.core.data.entity.TransactionEntity
import com.novafinance.core.domain.model.BackupBudgetDto
import com.novafinance.core.domain.model.BackupDebtDto
import com.novafinance.core.domain.model.BackupGoalDto
import com.novafinance.core.domain.model.BackupPayload
import com.novafinance.core.domain.model.BackupSourceDto
import com.novafinance.core.domain.model.BackupTransactionDto
import com.novafinance.core.domain.model.BalanceUpdateMode
import com.novafinance.core.domain.model.CURRENT_BACKUP_VERSION
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.InvalidBackupException
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.repository.BackupRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Works directly against the DAOs and entities rather than the four
 * per-feature repositories — those repositories' interfaces are
 * deliberately scoped (BudgetRepository only exposes one month at a
 * time, for example), and a full-database snapshot/restore is exactly
 * the kind of cross-cutting operation that scoping is meant to keep out
 * of everyday feature code.
 */
class BackupRepositoryImpl @Inject constructor(
    private val database: NovaDatabase,
    private val financialSourceDao: FinancialSourceDao,
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val goalDao: GoalDao,
    private val debtDao: DebtDao
) : BackupRepository {

    override suspend fun buildBackupPayload(): BackupPayload = BackupPayload(
        schemaVersion = CURRENT_BACKUP_VERSION,
        exportedAtEpochMillis = Instant.now().toEpochMilli(),
        sources = financialSourceDao.observeSources().first().map { it.toDto() },
        transactions = transactionDao.observeTransactions(accountId = null).first().map { it.toDto() },
        budgets = budgetDao.getAll().map { it.toDto() },
        goals = goalDao.observeGoals().first().map { it.toDto() },
        debts = debtDao.observeDebts().first().map { it.toDto() }
    )

    override suspend fun restoreFromPayload(payload: BackupPayload) {
        if (payload.schemaVersion > CURRENT_BACKUP_VERSION) {
            // Defense in depth — parseBackupPayload already checks this
            // before a payload ever reaches here, but a repository
            // should never trust its caller alone for a destructive
            // operation like wiping every table.
            throw InvalidBackupException("Unsupported backup version ${payload.schemaVersion}.")
        }

        database.withTransaction {
            financialSourceDao.deleteAll()
            transactionDao.deleteAll()
            budgetDao.deleteAll()
            goalDao.deleteAll()
            debtDao.deleteAll()

            payload.sources.forEach { financialSourceDao.insert(it.toEntity()) }
            payload.transactions.forEach { transactionDao.insert(it.toEntity()) }
            payload.budgets.forEach { budgetDao.upsert(it.toEntity()) }
            payload.goals.forEach { goalDao.insert(it.toEntity()) }
            payload.debts.forEach { debtDao.insert(it.toEntity()) }
        }
    }
}

private fun FinancialSourceEntity.toDto() = BackupSourceDto(
    id = id,
    name = name,
    type = type.name,
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    availableBalanceMinorUnits = availableBalanceMinorUnits,
    currency = currency,
    notes = notes,
    isActive = isActive,
    balanceUpdateMode = balanceUpdateMode.name,
    createdAtEpochMillis = createdAtEpochMillis
)

private fun BackupSourceDto.toEntity() = FinancialSourceEntity(
    id = id,
    name = name,
    type = runCatching { FinancialSourceType.valueOf(type) }.getOrDefault(FinancialSourceType.CUSTOM),
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    availableBalanceMinorUnits = availableBalanceMinorUnits,
    currency = currency,
    notes = notes,
    isActive = isActive,
    balanceUpdateMode = runCatching { BalanceUpdateMode.valueOf(balanceUpdateMode) }.getOrDefault(BalanceUpdateMode.MANUAL),
    creditLimitMinorUnits = null,
    includeInLiquidity = true,
    includeInSpendingPower = true,
    includeInForecast = true,
    includeInGoals = true,
    includeInAnalytics = true,
    isEmergencyReserve = false,
    groupId = null,
    linkedDebtId = null,
    createdAtEpochMillis = createdAtEpochMillis
)

private fun TransactionEntity.toDto() = BackupTransactionDto(
    id = id,
    accountId = accountId,
    merchant = merchant,
    category = category.name,
    amountMinorUnits = amountMinorUnits,
    date = date,
    note = note
)

private fun BackupTransactionDto.toEntity() = TransactionEntity(
    id = id,
    accountId = accountId,
    merchant = merchant,
    category = runCatching { TransactionCategory.valueOf(category) }.getOrDefault(TransactionCategory.OTHER),
    amountMinorUnits = amountMinorUnits,
    date = date,
    note = note
)

private fun BudgetEntity.toDto() = BackupBudgetDto(
    id = id,
    category = category.name,
    monthlyLimitMinorUnits = monthlyLimitMinorUnits,
    month = month
)

private fun BackupBudgetDto.toEntity() = BudgetEntity(
    id = id,
    category = runCatching { TransactionCategory.valueOf(category) }.getOrDefault(TransactionCategory.OTHER),
    monthlyLimitMinorUnits = monthlyLimitMinorUnits,
    month = month
)

private fun GoalEntity.toDto() = BackupGoalDto(
    id = id,
    name = name,
    targetAmountMinorUnits = targetAmountMinorUnits,
    currentAmountMinorUnits = currentAmountMinorUnits,
    targetDate = targetDate,
    createdAt = createdAt
)

private fun BackupGoalDto.toEntity() = GoalEntity(
    id = id,
    name = name,
    targetAmountMinorUnits = targetAmountMinorUnits,
    currentAmountMinorUnits = currentAmountMinorUnits,
    targetDate = targetDate,
    createdAt = createdAt
)

private fun DebtEntity.toDto() = BackupDebtDto(
    id = id,
    name = name,
    direction = direction.name,
    type = type.name,
    originalAmountMinorUnits = originalAmountMinorUnits,
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    interestRatePercent = interestRatePercent,
    minimumMonthlyPaymentMinorUnits = minimumMonthlyPaymentMinorUnits,
    dueDate = dueDate,
    counterpartyName = counterpartyName,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt
)

private fun BackupDebtDto.toEntity() = DebtEntity(
    id = id,
    name = name,
    direction = runCatching { DebtDirection.valueOf(direction) }.getOrDefault(DebtDirection.I_OWE),
    type = runCatching { DebtType.valueOf(type) }.getOrDefault(DebtType.OTHER),
    originalAmountMinorUnits = originalAmountMinorUnits,
    currentBalanceMinorUnits = currentBalanceMinorUnits,
    interestRatePercent = interestRatePercent,
    minimumMonthlyPaymentMinorUnits = minimumMonthlyPaymentMinorUnits,
    dueDate = dueDate,
    counterpartyName = counterpartyName,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt
)
