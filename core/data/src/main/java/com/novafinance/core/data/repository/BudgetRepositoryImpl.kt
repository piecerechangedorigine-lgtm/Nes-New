package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.BudgetDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {

    override fun observeBudgets(month: YearMonth): Flow<List<Budget>> =
        dao.observeBudgets(month.toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertBudget(budget: Budget) = dao.upsert(budget.toEntity())

    override suspend fun deleteBudget(budgetId: String) = dao.delete(budgetId)
}
