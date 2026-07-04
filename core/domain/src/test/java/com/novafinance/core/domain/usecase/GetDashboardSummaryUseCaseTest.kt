package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.fake.FakeFinancialSourceRepository
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.InsightTone
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetDashboardSummaryUseCaseTest {

    private val today = LocalDate.now()
    private val source = FinancialSource(
        id = "src-1",
        name = "Checking",
        type = FinancialSourceType.BANK_ACCOUNT,
        currentBalance = Money.fromMajor(2500.0),
        availableBalance = Money.fromMajor(2500.0),
        createdAt = Instant.now()
    )

    private fun transaction(amount: Double, category: TransactionCategory, date: LocalDate = today) = Transaction(
        id = "txn-${amount}-${category}-$date",
        accountId = source.id,
        merchant = "Merchant",
        category = category,
        amount = Money.fromMajor(amount),
        date = date
    )

    private fun useCase(sources: List<FinancialSource>, transactions: List<Transaction>) = GetDashboardSummaryUseCase(
        financialSourceRepository = FakeFinancialSourceRepository(sources),
        transactionRepository = FakeTransactionRepository(transactions),
        dispatcher = Dispatchers.Default
    )

    @Test
    fun `total balance sums every active source regardless of transaction activity`() = runTest {
        val secondSource = source.copy(id = "src-2", currentBalance = Money.fromMajor(500.0), availableBalance = Money.fromMajor(500.0))
        val useCase = useCase(sources = listOf(source, secondSource), transactions = emptyList())

        val result = useCase(Unit).first()

        assertThat(result).isInstanceOf(NovaResult.Success::class.java)
        val summary = (result as NovaResult.Success).data
        assertThat(summary.totalBalance).isEqualTo(Money.fromMajor(3000.0))
    }

    @Test
    fun `inactive sources are excluded from total balance`() = runTest {
        val closedSource = source.copy(id = "src-2", currentBalance = Money.fromMajor(9999.0), isActive = false)
        val useCase = useCase(sources = listOf(source, closedSource), transactions = emptyList())

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.totalBalance).isEqualTo(Money.fromMajor(2500.0))
    }

    @Test
    fun `income and expense only count this month's transactions`() = runTest {
        val lastMonth = YearMonth.from(today).minusMonths(1).atDay(1)
        val transactions = listOf(
            transaction(1000.0, TransactionCategory.INCOME, date = today),
            transaction(-200.0, TransactionCategory.FOOD, date = today),
            transaction(-9999.0, TransactionCategory.FOOD, date = lastMonth)
        )
        val useCase = useCase(sources = listOf(source), transactions = transactions)

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.monthIncome).isEqualTo(Money.fromMajor(1000.0))
        assertThat(summary.monthExpense).isEqualTo(Money.fromMajor(200.0))
    }

    @Test
    fun `spending more than earning surfaces a warning insight regardless of day of month`() = runTest {
        val transactions = listOf(
            transaction(500.0, TransactionCategory.INCOME),
            transaction(-800.0, TransactionCategory.SHOPPING)
        )
        val useCase = useCase(sources = listOf(source), transactions = transactions)

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.insight?.tone).isEqualTo(InsightTone.WARNING)
    }

    @Test
    fun `saving well over 20 percent of income surfaces a positive insight`() = runTest {
        // Expense is a small fraction of income — safely below the
        // pace-warning threshold on every possible day of the month, so
        // this always lands on the positive-savings branch.
        val transactions = listOf(
            transaction(1000.0, TransactionCategory.INCOME),
            transaction(-10.0, TransactionCategory.FOOD)
        )
        val useCase = useCase(sources = listOf(source), transactions = transactions)

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.insight?.tone).isEqualTo(InsightTone.POSITIVE)
    }

    @Test
    fun `no transactions this month means no insight at all`() = runTest {
        val useCase = useCase(sources = listOf(source), transactions = emptyList())

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.insight).isNull()
    }

    @Test
    fun `recent transactions are capped and most recent first`() = runTest {
        val transactions = (1..10).map { day ->
            transaction(-1.0 * day, TransactionCategory.OTHER, date = today.minusDays(day.toLong()))
        }
        val useCase = useCase(sources = listOf(source), transactions = transactions)

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.recentTransactions).hasSize(8)
        assertThat(summary.recentTransactions.first().date).isEqualTo(today.minusDays(1))
    }
}
