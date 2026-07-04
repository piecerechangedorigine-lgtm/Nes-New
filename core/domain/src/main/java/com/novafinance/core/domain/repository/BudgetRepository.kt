package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

interface BudgetRepository {
    fun observeBudgets(month: YearMonth): Flow<List<Budget>>
    suspend fun upsertBudget(budget: Budget)
    suspend fun deleteBudget(budgetId: String)
}
