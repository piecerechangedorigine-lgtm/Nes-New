package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.GoalDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val dao: GoalDao
) : GoalRepository {

    override fun observeGoals(): Flow<List<SavingsGoal>> =
        dao.observeGoals().map { list -> list.map { it.toDomain() } }

    override suspend fun addGoal(goal: SavingsGoal) = dao.insert(goal.toEntity())

    override suspend fun updateGoal(goal: SavingsGoal) = dao.update(goal.toEntity())

    override suspend fun contribute(goalId: String, amount: Money) = dao.contribute(goalId, amount.minorUnits)

    override suspend fun deleteGoal(goalId: String) = dao.delete(goalId)
}
