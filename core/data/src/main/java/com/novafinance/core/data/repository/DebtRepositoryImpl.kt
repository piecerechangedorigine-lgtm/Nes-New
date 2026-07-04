package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.DebtDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.repository.DebtRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DebtRepositoryImpl @Inject constructor(
    private val dao: DebtDao
) : DebtRepository {

    override fun observeDebts(): Flow<List<Debt>> =
        dao.observeDebts().map { list -> list.map { it.toDomain() } }

    override fun observeDebt(debtId: String): Flow<Debt?> =
        dao.observeDebt(debtId).map { it?.toDomain() }

    override suspend fun addDebt(debt: Debt) = dao.insert(debt.toEntity())

    override suspend fun updateDebt(debt: Debt) = dao.update(debt.toEntity())

    override suspend fun deleteDebt(debtId: String) = dao.delete(debtId)
}
