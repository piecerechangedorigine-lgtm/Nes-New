package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.AnalyticsSummary
import com.novafinance.core.domain.model.CategoryBreakdown
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.MonthlyTotal
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth
import javax.inject.Inject

private const val TREND_MONTHS = 6

/**
 * Every Analytics chart is a view onto the same transaction ledger the
 * rest of the app already uses — there's no separate "analytics event"
 * pipeline or pre-aggregated table to keep in sync. For the data volumes
 * a personal-finance app actually has (thousands of rows, not millions),
 * recomputing in-memory on each ledger change is simpler and cannot drift
 * from ground truth the way a maintained aggregate could.
 */
class GetAnalyticsSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, AnalyticsSummary>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<AnalyticsSummary>> =
        transactionRepository.observeTransactions(accountId = null).map { transactions ->
            val now = YearMonth.now()
            NovaResult.Success(
                AnalyticsSummary(
                    currentMonthBreakdown = categoryBreakdown(transactions, now),
                    monthlyTrend = monthlyTrend(transactions, now),
                    monthOverMonthExpenseChangePercent = monthOverMonthChange(transactions, now)
                )
            )
        }

    private fun categoryBreakdown(transactions: List<Transaction>, month: YearMonth): List<CategoryBreakdown> {
        val monthSpend = transactions.filter { YearMonth.from(it.date) == month && it.amount.isNegative }
        val totalSpend = Money.sum(monthSpend.map { -it.amount })
        if (totalSpend.minorUnits == 0L) return emptyList()

        return monthSpend
            .groupBy { it.category }
            .map { (category, categoryTransactions) ->
                val amount = Money.sum(categoryTransactions.map { -it.amount })
                CategoryBreakdown(
                    category = category,
                    amount = amount,
                    percentOfTotal = amount.ratioOf(totalSpend)
                )
            }
            .sortedByDescending { it.amount.minorUnits }
    }

    private fun monthlyTrend(transactions: List<Transaction>, currentMonth: YearMonth): List<MonthlyTotal> {
        val months = (0 until TREND_MONTHS).map { offset -> currentMonth.minusMonths((TREND_MONTHS - 1 - offset).toLong()) }
        return months.map { month ->
            val monthTransactions = transactions.filter { YearMonth.from(it.date) == month }
            MonthlyTotal(
                month = month,
                income = Money.sum(monthTransactions.filter { it.amount.isPositive }.map { it.amount }),
                expense = Money.sum(monthTransactions.filter { it.amount.isNegative }.map { -it.amount })
            )
        }
    }

    private fun monthOverMonthChange(transactions: List<Transaction>, currentMonth: YearMonth): Float? {
        val previousMonth = currentMonth.minusMonths(1)
        val currentExpense = Money.sum(
            transactions.filter { YearMonth.from(it.date) == currentMonth && it.amount.isNegative }.map { -it.amount }
        )
        val previousExpense = Money.sum(
            transactions.filter { YearMonth.from(it.date) == previousMonth && it.amount.isNegative }.map { -it.amount }
        )
        if (previousExpense.minorUnits == 0L) return null
        return (currentExpense.minorUnits - previousExpense.minorUnits).toFloat() / previousExpense.minorUnits.toFloat() * 100f
    }
}
