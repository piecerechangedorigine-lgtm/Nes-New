package com.novafinance.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.novafinance.app.notification.postFinancialHealthNotification
import com.novafinance.core.domain.model.ForecastStatus
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.repository.ProfileRepository
import com.novafinance.core.domain.usecase.GetDebtSummaryUseCase
import com.novafinance.core.domain.usecase.GetForecastSummaryUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import kotlinx.coroutines.flow.first

/**
 * One periodic worker doing two checks, not two separate periodic
 * workers — the "Debt Reminder Scheduling" and "Forecast Refresh Jobs"
 * the 11.5.3 brief lists as distinct requirements. Both are "does
 * something about the person's money need attention today" questions
 * answered from data this app already computes
 * ([GetDebtSummaryUseCase], [GetForecastSummaryUseCase]) — running one
 * combined job is the more battery-conscious choice the brief
 * explicitly asks for, at the cost of both checks always running
 * together rather than independently schedulable. Documented as a
 * deliberate consolidation in `FINANCIAL_SOURCES_ARCHITECTURE.md` and
 * `DEBT_ARCHITECTURE.md`, not an accidental simplification.
 *
 * Every check respects `ProfileSettings.areNotificationsEnabled` — this
 * worker still runs and still computes on schedule either way (so the
 * moment a person re-enables notifications, the next run reflects
 * current data immediately), it just doesn't post anything while the
 * person has notifications turned off. Freedom First: this automation
 * only ever *suggests* via a notification a person can dismiss or
 * ignore — it never modifies a debt, a source, or anything else on
 * its own.
 */
@HiltWorker
class FinancialHealthCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val profileRepository: ProfileRepository,
    private val getDebtSummary: GetDebtSummaryUseCase,
    private val getForecastSummary: GetForecastSummaryUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val settings = profileRepository.observeSettings().first()
        if (!settings.areNotificationsEnabled) return Result.success()

        checkDebts()
        checkForecast()
        Result.success()
    } catch (e: Exception) {
        Result.retry()
    }

    private suspend fun checkDebts() {
        val debtResult = getDebtSummary(Unit).first()
        val debts = (debtResult as? NovaResult.Success)?.data?.activeOwedDebts ?: return
        val today = LocalDate.now()
        val overdue = debts.filter { it.dueDate != null && it.dueDate.isBefore(today) }

        overdue.forEach { debt ->
            postFinancialHealthNotification(
                context = applicationContext,
                notificationId = ("debt_${debt.id}").hashCode(),
                title = "\"${debt.name}\" is past due",
                message = "This debt's due date has passed. Open Nova to review your payoff plan."
            )
        }
    }

    private suspend fun checkForecast() {
        val forecastResult = getForecastSummary(Unit).first()
        val forecast = (forecastResult as? NovaResult.Success)?.data ?: return

        if (forecast.status == ForecastStatus.DEFICIT) {
            postFinancialHealthNotification(
                context = applicationContext,
                notificationId = "forecast_deficit".hashCode(),
                title = "Your month is trending toward a deficit",
                message = forecast.message
            )
        }
    }
}
