package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGoalRepository(initial: List<SavingsGoal> = emptyList()) : GoalRepository {

    private val state = MutableStateFlow(initial)

    fun setGoals(goals: List<SavingsGoal>) {
        state.value = goals
    }

    override fun observeGoals(): Flow<List<SavingsGoal>> = state

    override suspend fun addGoal(goal: SavingsGoal) {
        state.value = state.value + goal
    }

    override suspend fun updateGoal(goal: SavingsGoal) {
        state.value = state.value.map { if (it.id == goal.id) goal else it }
    }

    override suspend fun contribute(goalId: String, amount: Money) {
        state.value = state.value.map { goal ->
            if (goal.id == goalId) goal.copy(currentAmount = goal.currentAmount + amount) else goal
        }
    }

    override suspend fun deleteGoal(goalId: String) {
        state.value = state.value.filterNot { it.id == goalId }
    }
}
