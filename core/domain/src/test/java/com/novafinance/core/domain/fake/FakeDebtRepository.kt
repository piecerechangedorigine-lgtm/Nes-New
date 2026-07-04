package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeDebtRepository(initial: List<Debt> = emptyList()) : DebtRepository {

    private val state = MutableStateFlow(initial)

    fun setDebts(debts: List<Debt>) {
        state.value = debts
    }

    override fun observeDebts(): Flow<List<Debt>> = state

    override fun observeDebt(debtId: String): Flow<Debt?> =
        state.map { debts -> debts.find { it.id == debtId } }

    override suspend fun addDebt(debt: Debt) {
        state.value = state.value + debt
    }

    override suspend fun updateDebt(debt: Debt) {
        state.value = state.value.map { if (it.id == debt.id) debt else it }
    }

    override suspend fun deleteDebt(debtId: String) {
        state.value = state.value.filterNot { it.id == debtId }
    }
}
