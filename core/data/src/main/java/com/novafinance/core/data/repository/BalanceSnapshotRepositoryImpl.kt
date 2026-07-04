package com.novafinance.core.data.repository

import com.novafinance.core.data.dao.BalanceSnapshotDao
import com.novafinance.core.data.entity.toDomain
import com.novafinance.core.data.entity.toEntity
import com.novafinance.core.domain.model.BalanceSnapshot
import com.novafinance.core.domain.repository.BalanceSnapshotRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BalanceSnapshotRepositoryImpl @Inject constructor(
    private val dao: BalanceSnapshotDao
) : BalanceSnapshotRepository {

    override fun observeSnapshots(sourceId: String): Flow<List<BalanceSnapshot>> =
        dao.observeSnapshots(sourceId).map { list -> list.map { it.toDomain() } }

    override fun observeAllSnapshots(): Flow<List<BalanceSnapshot>> =
        dao.observeAllSnapshots().map { list -> list.map { it.toDomain() } }

    override suspend fun recordSnapshot(snapshot: BalanceSnapshot) = dao.insert(snapshot.toEntity())
}
