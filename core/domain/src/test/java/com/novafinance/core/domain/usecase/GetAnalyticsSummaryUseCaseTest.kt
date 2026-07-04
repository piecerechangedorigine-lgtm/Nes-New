package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetAnalyticsSummaryUseCaseTest {

    private val thisMonth = YearMonth.now()
    private val today = LocalDate.now()

    private fun transaction(amount: Double, category: TransactionCategory, date: LocalDate) = Transaction(
        id = "txn-${amount}-${category}-$date",
        accountId = "acc-1",
        merchant = "Merchant",
        category = category,
        amount = Money.fromMajor(amount),
        date = date
    )

    private fun useCase(transactions: List<Transaction>) =
        GetAnalyticsSummaryUseCase(FakeTransactionRepository(transactions), Dispatchers.Default)

    @Test
    fun `category breakdown only counts spend not income and sums to 100 percent`() = runTest {
        val transactions = listOf(
            transaction(2000.0, TransactionCategory.INCOME, today),
            transaction(-300.0, TransactionCategory.FOOD, today),
            transaction(-100.0, TransactionCategory.TRANSPORT, today)
        )

        val summary = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(summary.currentMonthBreakdown).hasSize(2)
        val totalPercent = summary.currentMonthBreakdown.sumOf { it.percentOfTotal.toDouble() }
        assertThat(totalPercent).isWithin(0.001).of(1.0)
    }

    @Test
    fun `category breakdown is sorted largest spend first`() = runTest {
        val transactions = listOf(
            transaction(-50.0, TransactionCategory.TRANSPORT, today),
            transaction(-500.0, TransactionCategory.FOOD, today),
            transaction(-200.0, TransactionCategory.SHOPPING, today)
        )

        val summary = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(summary.currentMonthBreakdown.map { it.category }).containsExactly(
            TransactionCategory.FOOD,
            TransactionCategory.SHOPPING,
            TransactionCategory.TRANSPORT
        ).inOrder()
    }

    @Test
    fun `a month with no spend has an empty breakdown rather than dividing by zero`() = runTest {
        val summary = (useCase(emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(summary.currentMonthBreakdown).isEmpty()
    }

    @Test
    fun `monthly trend always covers exactly six months ending on the current month`() = runTest {
        val summary = (useCase(emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(summary.monthlyTrend).hasSize(6)
        assertThat(summary.monthlyTrend.last().month).isEqualTo(thisMonth)
        assertThat(summary.monthlyTrend.first().month).isEqualTo(thisMonth.minusMonths(5))
    }

    @Test
    fun `month over month change is null when there was no spend last month to compare against`() = runTest {
        val transactions = listOf(transaction(-100.0, TransactionCategory.FOOD, today))

        val summary = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(summary.monthOverMonthExpenseChangePercent).isNull()
    }

    @Test
    fun `month over month change reflects a real increase in spend`() = runTest {
        val lastMonth = thisMonth.minusMonths(1).atDay(1)
        val transactions = listOf(
            transaction(-100.0, TransactionCategory.FOOD, lastMonth),
            transaction(-150.0, TransactionCategory.FOOD, today)
        )

        val summary = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(summary.monthOverMonthExpenseChangePercent).isWithin(0.01f).of(50f)
    }
}
