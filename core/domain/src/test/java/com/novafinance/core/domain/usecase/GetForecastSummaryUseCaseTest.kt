package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.fake.FakeFinancialSourceRepository
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.ForecastStatus
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetForecastSummaryUseCaseTest {

    private val today = LocalDate.now()
    private val source = FinancialSource(
        id = "src-1",
        name = "Checking",
        type = FinancialSourceType.BANK_ACCOUNT,
        currentBalance = Money.fromMajor(1000.0),
        availableBalance = Money.fromMajor(1000.0),
        createdAt = Instant.now()
    )

    private fun transaction(amount: Double, category: TransactionCategory) = Transaction(
        id = "txn-${amount}-$category",
        accountId = source.id,
        merchant = "Merchant",
        category = category,
        amount = Money.fromMajor(amount),
        date = today
    )

    private fun useCase(transactions: List<Transaction>) = GetForecastSummaryUseCase(
        getDashboardSummary = GetDashboardSummaryUseCase(
            FakeFinancialSourceRepository(listOf(source)),
            FakeTransactionRepository(transactions),
            Dispatchers.Default
        ),
        dispatcher = Dispatchers.Default
    )

    @Test
    fun `expense far below income projects a clear surplus regardless of day of month`() = runTest {
        // Even fully extrapolated across a whole 31-day month, 1% of
        // income spent so far can never catch up to income by month end.
        val transactions = listOf(
            transaction(5000.0, TransactionCategory.INCOME),
            transaction(-1.0, TransactionCategory.FOOD)
        )

        val forecast = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(forecast.status).isEqualTo(ForecastStatus.SURPLUS)
        assertThat(forecast.message).contains("will save")
    }

    @Test
    fun `expense already exceeding income projects a clear deficit regardless of day of month`() = runTest {
        val transactions = listOf(
            transaction(500.0, TransactionCategory.INCOME),
            transaction(-2000.0, TransactionCategory.SHOPPING)
        )

        val forecast = (useCase(transactions)(Unit).first() as NovaResult.Success).data

        assertThat(forecast.status).isEqualTo(ForecastStatus.DEFICIT)
        assertThat(forecast.message).contains("deficit")
    }

    @Test
    fun `no transactions at all projects breakeven with no error`() = runTest {
        val forecast = (useCase(emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(forecast.status).isEqualTo(ForecastStatus.ON_TRACK)
        assertThat(forecast.projectedSurplusOrDeficit).isEqualTo(Money.ZERO)
    }
}
