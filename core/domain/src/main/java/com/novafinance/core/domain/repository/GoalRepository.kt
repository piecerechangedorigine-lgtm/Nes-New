package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun observeGoals(): Flow<List<SavingsGoal>>
    suspend fun addGoal(goal: SavingsGoal)
    suspend fun updateGoal(goal: SavingsGoal)
    suspend fun contribute(goalId: String, amount: Money)
    suspend fun deleteGoal(goalId: String)
}
