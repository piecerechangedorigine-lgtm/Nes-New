package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.FinancialSource
import kotlinx.coroutines.flow.Flow

/**
 * All sources are user-entered and locally persisted — there is no
 * bank-linking provider in v1, so this interface has no notion of
 * "sync" or "refresh" today. [FinancialSource.balanceUpdateMode] is what
 * a future SMS/OCR-driven implementation hangs off of without this
 * interface's shape needing to change.
 */
interface FinancialSourceRepository {
    /** All sources, active and inactive — screens that need to filter (e.g. the picker in Add Transaction) filter on [FinancialSource.isActive] themselves. */
    fun observeSources(): Flow<List<FinancialSource>>
    fun observeSource(sourceId: String): Flow<FinancialSource?>
    suspend fun addSource(source: FinancialSource)
    suspend fun updateSource(source: FinancialSource)
    suspend fun deleteSource(sourceId: String)
}
