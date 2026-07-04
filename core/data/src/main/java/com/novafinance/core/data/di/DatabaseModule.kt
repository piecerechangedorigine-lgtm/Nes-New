package com.novafinance.core.data.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.novafinance.core.data.dao.BalanceSnapshotDao
import com.novafinance.core.data.dao.BudgetDao
import com.novafinance.core.data.dao.DebtDao
import com.novafinance.core.data.dao.FinancialSourceDao
import com.novafinance.core.data.dao.GoalDao
import com.novafinance.core.data.dao.TransactionDao
import com.novafinance.core.data.database.NovaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DATABASE_NAME = "nova.db"

/**
 * Index-only schema change: merges TransactionEntity's separate `date`
 * and `category` indices into one composite (date, category) index, and
 * adds the unique (month, category) index BudgetEntity had no index of
 * any kind for before. Neither table's columns or data change, so this
 * migration is pure DDL — no data copy, no data loss risk.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP INDEX IF EXISTS index_transactions_date")
        db.execSQL("DROP INDEX IF EXISTS index_transactions_category")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_date_category " +
                "ON transactions(date, category)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_month_category " +
                "ON budgets(month, category)"
        )
    }
}

/**
 * Renames "accounts" to "financial_sources" and adds the columns the
 * expanded FinancialSource model needs (Phase 8.5.2/8.5.3). Deliberately
 * built from only `RENAME TO` / `ADD COLUMN` / `UPDATE` statements — no
 * `RENAME COLUMN` and no table recreation — because `RENAME COLUMN`
 * isn't supported on the SQLite version bundled with early Android 8.x
 * devices at this app's minSdk (26); every statement here is safe on
 * every supported OS version.
 *
 * The old AccountType values CHECKING/SAVINGS/INVESTMENT don't exist in
 * the new FinancialSourceType enum, so existing rows get explicitly
 * remapped (CHECKING -> BANK_ACCOUNT, SAVINGS -> SAVINGS_ACCOUNT,
 * INVESTMENT -> CUSTOM, since the new type set has no direct investment
 * equivalent) rather than left to fail `valueOf()` on next read.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts RENAME TO financial_sources")

        db.execSQL("ALTER TABLE financial_sources ADD COLUMN availableBalanceMinorUnits INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN notes TEXT")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN balanceUpdateMode TEXT NOT NULL DEFAULT 'MANUAL'")

        // Available balance starts equal to current balance for every
        // existing source — the two only diverge once a real distinction
        // (e.g. a credit line) is entered by the person.
        db.execSQL("UPDATE financial_sources SET availableBalanceMinorUnits = balanceMinorUnits")

        db.execSQL(
            """
            UPDATE financial_sources SET type = CASE type
                WHEN 'CHECKING' THEN 'BANK_ACCOUNT'
                WHEN 'SAVINGS' THEN 'SAVINGS_ACCOUNT'
                WHEN 'INVESTMENT' THEN 'CUSTOM'
                ELSE type
            END
            """.trimIndent()
        )
    }
}

/**
 * A brand new table for Phase 10's Debt Intelligence Center — no
 * existing data to migrate, just `CREATE TABLE`. Column types mirror
 * [com.novafinance.core.data.entity.DebtEntity] exactly; `direction`
 * and `type` are stored as `TEXT` (enum `.name`, same convention as
 * every other enum column — see [com.novafinance.core.data.Converters]).
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debts (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                direction TEXT NOT NULL,
                type TEXT NOT NULL,
                originalAmountMinorUnits INTEGER NOT NULL,
                currentBalanceMinorUnits INTEGER NOT NULL,
                interestRatePercent REAL,
                minimumMonthlyPaymentMinorUnits INTEGER,
                dueDate TEXT,
                counterpartyName TEXT,
                notes TEXT,
                isActive INTEGER NOT NULL,
                createdAt TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debts_direction ON debts(direction)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debts_isActive ON debts(isActive)")
    }
}

/**
 * Phase 11's `financial_sources` expansion (credit card limit, the
 * five 11.4 inclusion flags, emergency-reserve flag, group linkage)
 * plus the brand new `balance_snapshots` table (11.8). Same rule as
 * every migration this table has had: `ADD COLUMN` only, no
 * `RENAME COLUMN`, for SQLite-version safety on early Android 8.x
 * devices.
 *
 * Every new boolean inclusion flag defaults to `1` (true) — an
 * existing source, migrated from before Phase 11 existed, keeps
 * counting toward every calculation exactly as it always implicitly
 * did, rather than silently dropping out of Total Liquidity or
 * Spending Power the moment this migration runs. [isEmergencyReserve]
 * is the one flag that defaults to `0` (false), since that's a new,
 * explicit fact only the person can declare — there's no previous
 * behavior for it to preserve.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN creditLimitMinorUnits INTEGER")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN includeInLiquidity INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN includeInSpendingPower INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN includeInForecast INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN includeInGoals INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN includeInAnalytics INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN isEmergencyReserve INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN groupId TEXT")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS balance_snapshots (
                id TEXT NOT NULL PRIMARY KEY,
                sourceId TEXT NOT NULL,
                balanceMinorUnits INTEGER NOT NULL,
                recordedAtEpochMillis INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_balance_snapshots_sourceId ON balance_snapshots(sourceId)")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNovaDatabase(@ApplicationContext context: Context): NovaDatabase =
        // No fallbackToDestructiveMigration() — a missing Migration fails
        // loudly instead of silently deleting the user's financial data.
        Room.databaseBuilder(context, NovaDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    fun provideFinancialSourceDao(database: NovaDatabase): FinancialSourceDao = database.financialSourceDao()

    @Provides
    fun provideTransactionDao(database: NovaDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideBudgetDao(database: NovaDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideGoalDao(database: NovaDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideDebtDao(database: NovaDatabase): DebtDao = database.debtDao()

    @Provides
    fun provideBalanceSnapshotDao(database: NovaDatabase): BalanceSnapshotDao = database.balanceSnapshotDao()
}

/**
 * Phase 11.5.1's reconciliation link — one nullable column, no data to
 * migrate (every existing source is simply unlinked, matching real
 * behavior before this phase existed).
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE financial_sources ADD COLUMN linkedDebtId TEXT")
    }
}
