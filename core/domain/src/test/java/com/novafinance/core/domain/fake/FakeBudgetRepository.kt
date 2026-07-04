package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.repository.BudgetRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBudgetRepository(initial: List<Budget> = emptyList()) : BudgetRepository {

    private val state = MutableStateFlow(initial)

    fun setBudgets(budgets: List<Budget>) {
        state.value = budgets
    }

    override fun observeBudgets(month: YearMonth): Flow<List<Budget>> =
        state.map { budgets -> budgets.filter { it.month == month } }

    override suspend fun upsertBudget(budget: Budget) {
        state.value = state.value.filterNot { it.id == budget.id } + budget
    }

    override suspend fun deleteBudget(budgetId: String) {
        state.value = state.value.filterNot { it.id == budgetId }
    }
}
