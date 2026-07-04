package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.ForecastConfidence
import com.novafinance.core.domain.model.ForecastStatus
import com.novafinance.core.domain.model.ForecastSummary
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.formatted
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Extrapolates this month's spending pace forward across the remaining
 * days — a linear projection from spend-so-far, deliberately not a
 * trend fit against history (that's Analytics' job). Built on top of
 * [GetDashboardSummaryUseCase] rather than re-querying repositories, so
 * this can never disagree with the numbers Dashboard already shows for
 * income/expense/balance — only the forward-looking math is new here.
 *
 * Income is treated as already fully realized for the month (no
 * forward extrapolation) — only expense pace is projected forward. For
 * a salaried person whose pay already landed this month, that's the
 * right assumption; a future version that models recurring income
 * separately from one-off income could relax it, but nothing in the
 * current Transaction model distinguishes the two yet.
 */
class GetForecastSummaryUseCase @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, ForecastSummary>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<ForecastSummary>> =
        getDashboardSummary(Unit).map { result ->
            when (result) {
                is NovaResult.Success -> NovaResult.Success(
                    buildForecast(
                        income = result.data.monthIncome,
                        expense = result.data.monthExpense,
                        currentBalance = result.data.totalBalance,
                        today = LocalDate.now()
                    )
                )
                is NovaResult.Error -> result
                is NovaResult.Loading -> NovaResult.Loading
            }
        }

    private fun buildForecast(income: Money, expense: Money, currentBalance: Money, today: LocalDate): ForecastSummary {
        val daysInMonth = today.lengthOfMonth()
        val daysSoFar = today.dayOfMonth
        val remainingDays = (daysInMonth - daysSoFar).coerceAtLeast(0)

        val dailyExpenseRate = if (daysSoFar > 0) expense.minorUnits.toDouble() / daysSoFar else 0.0
        val projectedAdditionalExpense = Money(Math.round(dailyExpenseRate * remainingDays))

        val projectedTotalExpense = expense + projectedAdditionalExpense
        val projectedEndOfMonthBalance = currentBalance - projectedAdditionalExpense
        val surplusOrDeficit = income - projectedTotalExpense

        val status = statusFor(surplusOrDeficit, income)
        val confidence = confidenceFor(daysSoFar, daysInMonth)
        val message = messageFor(status, surplusOrDeficit)

        return ForecastSummary(
            projectedEndOfMonthBalance = projectedEndOfMonthBalance,
            projectedSurplusOrDeficit = surplusOrDeficit,
            status = status,
            confidence = confidence,
            message = message
        )
    }

    /** Within roughly 1% of income either way counts as "on track" rather than a false-precision surplus/deficit call on essentially a wash. */
    private fun statusFor(surplusOrDeficit: Money, income: Money): ForecastStatus {
        val breakEvenBand = if (income.minorUnits > 0) (income.minorUnits * 0.01).toLong() else 0L
        return when {
            surplusOrDeficit.minorUnits > breakEvenBand -> ForecastStatus.SURPLUS
            surplusOrDeficit.minorUnits < -breakEvenBand -> ForecastStatus.DEFICIT
            else -> ForecastStatus.ON_TRACK
        }
    }

    private fun confidenceFor(daysSoFar: Int, daysInMonth: Int): ForecastConfidence {
        val elapsedRatio = daysSoFar.toDouble() / daysInMonth
        return when {
            elapsedRatio < 0.2 -> ForecastConfidence.LOW
            elapsedRatio < 0.5 -> ForecastConfidence.MEDIUM
            else -> ForecastConfidence.HIGH
        }
    }

    private fun messageFor(status: ForecastStatus, surplusOrDeficit: Money): String {
        val magnitude = if (surplusOrDeficit.isNegative) -surplusOrDeficit else surplusOrDeficit
        return when (status) {
            ForecastStatus.SURPLUS -> "At current spending pace you will save ${magnitude.formatted()} this month."
            ForecastStatus.DEFICIT -> "At current spending pace you may end the month with a ${magnitude.formatted()} deficit."
            ForecastStatus.ON_TRACK -> "At current spending pace you're on track to break even this month."
        }
    }
}
