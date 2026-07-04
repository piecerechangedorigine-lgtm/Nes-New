package com.novafinance.core.domain.fake

import com.novafinance.core.domain.model.BalanceSnapshot
import com.novafinance.core.domain.repository.BalanceSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeBalanceSnapshotRepository(initial: List<BalanceSnapshot> = emptyList()) : BalanceSnapshotRepository {

    private val state = MutableStateFlow(initial)

    override fun observeSnapshots(sourceId: String): Flow<List<BalanceSnapshot>> =
        state.map { snapshots -> snapshots.filter { it.sourceId == sourceId } }

    override fun observeAllSnapshots(): Flow<List<BalanceSnapshot>> = state

    override suspend fun recordSnapshot(snapshot: BalanceSnapshot) {
        state.value = state.value + snapshot
    }
}
