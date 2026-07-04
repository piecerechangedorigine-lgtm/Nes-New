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

@HiltWorker
class FinancialHealthCheckWorker @AssistedInject constructor(
@Assisted context: Context,
@Assisted params: WorkerParameters,
private val profileRepository: ProfileRepository,
private val getDebtSummary: GetDebtSummaryUseCase,
private val getForecastSummary: GetForecastSummaryUseCase
) : CoroutineWorker(context, params) {

override suspend fun doWork(): Result {
    return try {
        val settings = profileRepository.observeSettings().first()

        if (!settings.areNotificationsEnabled) {
            Result.success()
        } else {
            checkDebts()
            checkForecast()
            Result.success()
        }
    } catch (e: Exception) {
        Result.retry()
    }
}

private suspend fun checkDebts() {
    val debtResult = getDebtSummary(Unit).first()
    val debts = (debtResult as? NovaResult.Success)?.data?.activeOwedDebts ?: return

    val today = LocalDate.now()

    val overdue = debts.filter { debt ->
        debt.dueDate?.isBefore(today) == true
    }

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
