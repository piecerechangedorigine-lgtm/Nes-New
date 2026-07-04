package com.novafinance.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.novafinance.app.notification.ensureNotificationChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

private const val WORK_NAME_BALANCE_SNAPSHOT = "balance_snapshot"
private const val WORK_NAME_FINANCIAL_HEALTH_CHECK = "financial_health_check"

/**
 * Everything about *how* the two Phase 11.5.3 workers get scheduled
 * lives here, separate from *what* they do (the workers themselves) —
 * so changing the frequency or constraints never means touching
 * `doWork()`.
 *
 * Battery-conscious scheduling (an explicit 11.5.3 requirement), concretely:
 * - Both jobs are periodic at 24 hours, WorkManager's Doze-friendly
 *   default cadence — no job here ever needs sub-daily freshness, so
 *   there's no reason to burn more wake-ups than that.
 * - [Constraints.setRequiresBatteryNotLow] on both — neither snapshot
 *   recording nor a notification check is urgent enough to justify
 *   running while the device is critically low on battery.
 * - No network constraint on either — everything both workers read is
 *   already local (Room/DataStore), so requiring connectivity would
 *   only add unnecessary wait time with zero actual benefit.
 * - [ExistingPeriodicWorkPolicy.KEEP] on every enqueue — calling
 *   `scheduleAll()` again (every app launch, via `NovaApplication`)
 *   never cancels and re-creates an already-scheduled job, which would
 *   otherwise reset its next-run countdown on every single app open.
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun scheduleAll() {
        ensureNotificationChannel(context)
        scheduleBalanceSnapshots()
        scheduleFinancialHealthCheck()
    }

    /**
     * Daily by default — the 11.5.3 brief lists "Daily Balance
     * Snapshots" and "Weekly Balance Snapshots" as separate
     * requirements, but running both simultaneously would just double
     * every source's snapshot history for no analytical benefit (a
     * weekly trend is trivially derivable from daily data by sampling
     * every 7th point — see `SourceAnalytics`). One daily periodic job
     * is both the more battery-conscious choice and produces a strict
     * superset of what a separate weekly job would capture. See
     * `FINANCIAL_SOURCES_ARCHITECTURE.md` for the full reasoning.
     */
    private fun scheduleBalanceSnapshots() {
        val request = PeriodicWorkRequestBuilder<BalanceSnapshotWorker>(Duration.ofHours(24))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_BALANCE_SNAPSHOT, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun scheduleFinancialHealthCheck() {
        val request = PeriodicWorkRequestBuilder<FinancialHealthCheckWorker>(Duration.ofHours(24))
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofMinutes(15))
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_FINANCIAL_HEALTH_CHECK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
