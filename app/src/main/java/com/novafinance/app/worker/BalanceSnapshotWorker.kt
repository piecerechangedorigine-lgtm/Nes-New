package com.novafinance.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novafinance.core.domain.model.BalanceSnapshot
import com.novafinance.core.domain.repository.BalanceSnapshotRepository
import com.novafinance.core.domain.repository.FinancialSourceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first

/**
 * Records one [BalanceSnapshot] per active [com.novafinance.core.domain.model.FinancialSource]
 * every time it runs — this is the first thing that actually calls
 * [BalanceSnapshotRepository.recordSnapshot] since Phase 11 built the
 * storage for it with nothing populating it. See `WorkScheduler` for
 * the actual frequency this runs at and why daily/weekly is a person's
 * choice rather than two simultaneous jobs.
 */
@HiltWorker
class BalanceSnapshotWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val financialSourceRepository: FinancialSourceRepository,
    private val balanceSnapshotRepository: BalanceSnapshotRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val sources = financialSourceRepository.observeSources().first().filter { it.isActive }
        val now = Instant.now()
        sources.forEach { source ->
            balanceSnapshotRepository.recordSnapshot(
                BalanceSnapshot(id = UUID.randomUUID().toString(), sourceId = source.id, balance = source.currentBalance, recordedAt = now)
            )
        }
        Result.success()
    } catch (e: Exception) {
        // Retries per WorkScheduler's BackoffPolicy — a transient
        // failure (e.g. the database briefly locked by another write)
        // shouldn't need to wait for the next scheduled run.
        Result.retry()
    }
}
