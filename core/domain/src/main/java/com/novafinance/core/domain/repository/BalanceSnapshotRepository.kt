package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.BalanceSnapshot
import kotlinx.coroutines.flow.Flow

interface BalanceSnapshotRepository {
    fun observeSnapshots(sourceId: String): Flow<List<BalanceSnapshot>>
    /** Every snapshot across every source — what Source Analytics' Liquidity Trend and Savings Growth charts (11.5.2) aggregate over, since a portfolio-level trend needs every source's history, not one at a time. */
    fun observeAllSnapshots(): Flow<List<BalanceSnapshot>>
    suspend fun recordSnapshot(snapshot: BalanceSnapshot)
}
