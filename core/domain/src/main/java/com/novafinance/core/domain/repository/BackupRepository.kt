package com.novafinance.core.domain.repository

import com.novafinance.core.domain.model.BackupPayload

/**
 * Both methods are plain suspend functions, not `Flow`-returning — a
 * backup export/import is a one-shot action with a clear start and end,
 * not an ongoing stream of state to observe. See ExportBackupUseCase /
 * ImportBackupUseCase for where this gets wrapped in NovaResult.
 */
interface BackupRepository {
    suspend fun buildBackupPayload(): BackupPayload

    /**
     * Replaces every FinancialSource/Transaction/Budget/Goal row with
     * exactly what's in [payload], atomically — a partial import (e.g.
     * sources written but transactions failing halfway through) would
     * leave foreign-key references dangling, so this either fully
     * succeeds or leaves the existing data completely untouched.
     */
    suspend fun restoreFromPayload(payload: BackupPayload)
}
