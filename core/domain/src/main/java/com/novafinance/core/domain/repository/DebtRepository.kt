package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.Debt
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    /** Every debt, both directions, active and settled — screens filter as needed, same pattern as [FinancialSourceRepository.observeSources]. */
    fun observeDebts(): Flow<List<Debt>>
    fun observeDebt(debtId: String): Flow<Debt?>
    suspend fun addDebt(debt: Debt)
    suspend fun updateDebt(debt: Debt)
    suspend fun deleteDebt(debtId: String)
}
