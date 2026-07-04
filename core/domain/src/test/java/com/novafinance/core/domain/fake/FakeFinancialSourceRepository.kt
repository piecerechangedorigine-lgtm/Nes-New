package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.repository.FinancialSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeFinancialSourceRepository(initial: List<FinancialSource> = emptyList()) : FinancialSourceRepository {

    private val state = MutableStateFlow(initial)

    fun setSources(sources: List<FinancialSource>) {
        state.value = sources
    }

    override fun observeSources(): Flow<List<FinancialSource>> = state

    override fun observeSource(sourceId: String): Flow<FinancialSource?> =
        state.map { sources -> sources.find { it.id == sourceId } }

    override suspend fun addSource(source: FinancialSource) {
        state.value = state.value + source
    }

    override suspend fun updateSource(source: FinancialSource) {
        state.value = state.value.map { if (it.id == source.id) source else it }
    }

    override suspend fun deleteSource(sourceId: String) {
        state.value = state.value.filterNot { it.id == sourceId }
    }
}
