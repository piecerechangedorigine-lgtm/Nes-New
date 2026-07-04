package com.novafinance.core.data.di

import com.novafinance.core.data.repository.BackupRepositoryImpl
import com.novafinance.core.data.repository.BalanceSnapshotRepositoryImpl
import com.novafinance.core.data.repository.BudgetRepositoryImpl
import com.novafinance.core.data.repository.DashboardRepositoryImpl
import com.novafinance.core.data.repository.DebtRepositoryImpl
import com.novafinance.core.data.repository.FinancialSourceRepositoryImpl
import com.novafinance.core.data.repository.GoalRepositoryImpl
import com.novafinance.core.data.repository.PermissionRepositoryImpl
import com.novafinance.core.data.repository.ProfileRepositoryImpl
import com.novafinance.core.data.repository.SourceGroupRepositoryImpl
import com.novafinance.core.data.repository.TransactionRepositoryImpl
import com.novafinance.core.domain.repository.BackupRepository
import com.novafinance.core.domain.repository.BalanceSnapshotRepository
import com.novafinance.core.domain.repository.BudgetRepository
import com.novafinance.core.domain.repository.DashboardRepository
import com.novafinance.core.domain.repository.DebtRepository
import com.novafinance.core.domain.repository.FinancialSourceRepository
import com.novafinance.core.domain.repository.GoalRepository
import com.novafinance.core.domain.repository.PermissionRepository
import com.novafinance.core.domain.repository.ProfileRepository
import com.novafinance.core.domain.repository.SourceGroupRepository
import com.novafinance.core.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFinancialSourceRepository(impl: FinancialSourceRepositoryImpl): FinancialSourceRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindPermissionRepository(impl: PermissionRepositoryImpl): PermissionRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindDebtRepository(impl: DebtRepositoryImpl): DebtRepository

    @Binds
    @Singleton
    abstract fun bindBalanceSnapshotRepository(impl: BalanceSnapshotRepositoryImpl): BalanceSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindSourceGroupRepository(impl: SourceGroupRepositoryImpl): SourceGroupRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindGoalRepository(impl: GoalRepositoryImpl): GoalRepository
}
