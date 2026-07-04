package com.novafinance.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.novafinance.core.data.Converters
import com.novafinance.core.data.dao.BalanceSnapshotDao
import com.novafinance.core.data.dao.BudgetDao
import com.novafinance.core.data.dao.DebtDao
import com.novafinance.core.data.dao.FinancialSourceDao
import com.novafinance.core.data.dao.GoalDao
import com.novafinance.core.data.dao.TransactionDao
import com.novafinance.core.data.entity.BalanceSnapshotEntity
import com.novafinance.core.data.entity.BudgetEntity
import com.novafinance.core.data.entity.DebtEntity
import com.novafinance.core.data.entity.FinancialSourceEntity
import com.novafinance.core.data.entity.GoalEntity
import com.novafinance.core.data.entity.TransactionEntity

/**
 * Single Room database for the whole app. Each feature module owns its own
 * Entity/Dao pair under core:data and registers it here — the database
 * itself stays a thin aggregation point with zero business logic.
 *
 * version = 6 — Phase 11.5.1 added `linkedDebtId` to `financial_sources`
 * for the FinancialSource-Debt reconciliation layer. version 5 was
 * Phase 11's credit card + inclusion-control columns and the new
 * `balance_snapshots` table. See MIGRATION_5_6 in DatabaseModule. Any
 * future column change ships its own explicit Migration; Room is
 * configured with no destructive fallback (see DatabaseModule), so a
 * missing migration fails loudly in debug builds instead of silently
 * wiping user data in production.
 */
@Database(
    entities = [
        FinancialSourceEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        DebtEntity::class,
        BalanceSnapshotEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun financialSourceDao(): FinancialSourceDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun debtDao(): DebtDao
    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
}
