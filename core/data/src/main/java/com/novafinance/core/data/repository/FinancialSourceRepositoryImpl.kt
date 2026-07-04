package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.FinancialSourceDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.repository.FinancialSourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FinancialSourceRepositoryImpl @Inject constructor(
    private val dao: FinancialSourceDao
) : FinancialSourceRepository {

    override fun observeSources(): Flow<List<FinancialSource>> =
        dao.observeSources().map { list -> list.map { it.toDomain() } }

    override fun observeSource(sourceId: String): Flow<FinancialSource?> =
        dao.observeSource(sourceId).map { it?.toDomain() }

    override suspend fun addSource(source: FinancialSource) = dao.insert(source.toEntity())

    override suspend fun updateSource(source: FinancialSource) = dao.update(source.toEntity())

    override suspend fun deleteSource(sourceId: String) = dao.delete(sourceId)
}
