package com.novafinance.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.novafinance.app.worker.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point. Hilt's dependency graph is rooted here.
 *
 * [Configuration.Provider] is what lets WorkManager construct
 * [com.novafinance.app.worker.BalanceSnapshotWorker] and
 * [com.novafinance.app.worker.FinancialHealthCheckWorker] through
 * Hilt (via [HiltWorkerFactory]) rather than their own no-arg
 * constructors — both need injected repositories/use cases, the same
 * way every ViewModel in this app does. The manifest disables
 * WorkManager's own default initializer (see AndroidManifest.xml) so
 * this is the only place WorkManager gets configured, avoiding the
 * double-initialization crash that setup otherwise causes.
 */
@HiltAndroidApp
class NovaApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        workScheduler.scheduleAll()
    }
}
