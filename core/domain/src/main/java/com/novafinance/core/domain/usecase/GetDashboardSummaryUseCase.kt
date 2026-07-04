package com.novafinance.core.domain.usecase

import com.novafinance.core.domain.model.DashboardInsight
import com.novafinance.core.domain.model.DashboardSummary
import com.novafinance.core.domain.model.InsightTone
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.repository.FinancialSourceRepository
import com.novafinance.core.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Combines live financial sources + this-month transactions into the
 * single [DashboardSummary] the Dashboard hero card renders. This is the
 * one piece of dashboard logic complex enough to earn a use case rather
 * than living in the ViewModel — everything else on that screen is a
 * direct repository read.
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val financialSourceRepository: FinancialSourceRepository,
    private val transactionRepository: TransactionRepository,
    dispatcher: CoroutineDispatcher
) : NovaFlowUseCase<Unit, DashboardSummary>(dispatcher) {

    override fun execute(params: Unit): Flow<NovaResult<DashboardSummary>> {
        val thisMonth = YearMonth.now()
        return combine(
            financialSourceRepository.observeSources(),
            transactionRepository.observeTransactionsForMonth(thisMonth),
            transactionRepository.observeRecentTransactions(limit = 8)
        ) { sources, monthTransactions, recent ->
            // Closed/archived sources don't count toward the headline
            // balance — see BalanceOverview (GetBalanceOverviewUseCase)
            // for the fuller liability-aware liquidity calculation.
            val totalBalance = Money.sum(sources.filter { it.isActive }.map { it.currentBalance })
            val income = Money.sum(monthTransactions.filter { it.amount.isPositive }.map { it.amount })
            val expense = Money.sum(monthTransactions.filter { it.amount.isNegative }.map { it.amount })

            NovaResult.Success(
                DashboardSummary(
                    totalBalance = totalBalance,
                    monthIncome = income,
                    monthExpense = -expense,
                    recentTransactions = recent,
                    insight = buildInsight(income = income, expense = -expense, transactions = monthTransactions)
                )
            )
        }
    }

    /**
     * Deliberately simple, explainable heuristics — not a model, not a
     * trend fit against history (that lands with Analytics in Phase 5).
     * Each branch is a fact directly checkable against [transactions].
     */
    private fun buildInsight(income: Money, expense: Money, transactions: List<Transaction>): DashboardInsight? {
        if (transactions.isEmpty()) return null

        if (expense > income && income.isPositive) {
            return DashboardInsight(
                message = "You've spent more than you've earned this month.",
                tone = InsightTone.WARNING
            )
        }

        val daysSoFar = ChronoUnit.DAYS.between(LocalDate.now().withDayOfMonth(1), LocalDate.now()) + 1
        val daysInMonth = LocalDate.now().lengthOfMonth()
        val paceRatio = if (daysInMonth > 0) daysSoFar.toDouble() / daysInMonth else 1.0
        if (income.minorUnits > 0 && expense.toMajorDouble() > income.toMajorDouble() * 0.8 * paceRatio) {
            return DashboardInsight(
                message = "You're on pace to spend most of this month's income.",
                tone = InsightTone.WARNING
            )
        }

        if (income.isPositive && expense.isPositive) {
            val savedRatio = 1 - (expense.toMajorDouble() / income.toMajorDouble())
            if (savedRatio > 0.2) {
                return DashboardInsight(
                    message = "You're on track to save over 20% of your income this month.",
                    tone = InsightTone.POSITIVE
                )
            }
        }

        return DashboardInsight(
            message = "Spending is steady so far this month.",
            tone = InsightTone.NEUTRAL
        )
    }
}
