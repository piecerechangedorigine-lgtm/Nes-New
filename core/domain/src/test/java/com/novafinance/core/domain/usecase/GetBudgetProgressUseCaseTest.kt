package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.fake.FakeBudgetRepository
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.Budget
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

class GetBudgetProgressUseCaseTest {

    private val month = YearMonth.of(2026, 3)
    private val today = LocalDate.of(2026, 3, 15)

    private fun transaction(amount: Double, category: TransactionCategory) = Transaction(
        id = "txn-${amount}-$category",
        accountId = "acc-1",
        merchant = "Merchant",
        category = category,
        amount = Money.fromMajor(amount),
        date = today
    )

    private fun useCase(budgets: List<Budget>, transactions: List<Transaction>) = GetBudgetProgressUseCase(
        budgetRepository = FakeBudgetRepository(budgets),
        transactionRepository = FakeTransactionRepository(transactions),
        dispatcher = Dispatchers.Default
    )

    @Test
    fun `spent only counts negative transactions in the budget's own category`() = runTest {
        val budget = Budget(id = "b1", category = TransactionCategory.FOOD, monthlyLimit = Money.fromMajor(300.0), month = month)
        val transactions = listOf(
            transaction(-100.0, TransactionCategory.FOOD),
            transaction(-50.0, TransactionCategory.FOOD),
            transaction(-9999.0, TransactionCategory.TRANSPORT), // different category, ignored
            transaction(200.0, TransactionCategory.FOOD) // income/refund, ignored
        )

        val progress = (useCase(listOf(budget), transactions)(month).first() as NovaResult.Success).data

        assertThat(progress).hasSize(1)
        assertThat(progress.single().spent).isEqualTo(Money.fromMajor(150.0))
    }

    @Test
    fun `isOverLimit and remaining reflect actual spend against the limit`() = runTest {
        val budget = Budget(id = "b1", category = TransactionCategory.SHOPPING, monthlyLimit = Money.fromMajor(100.0), month = month)
        val transactions = listOf(transaction(-150.0, TransactionCategory.SHOPPING))

        val progress = (useCase(listOf(budget), transactions)(month).first() as NovaResult.Success).data.single()

        assertThat(progress.isOverLimit).isTrue()
        assertThat(progress.remaining).isEqualTo(Money.fromMajor(-50.0))
        assertThat(progress.percentUsed).isEqualTo(1f)
    }

    @Test
    fun `a budget with no matching transactions has zero spend not an error`() = runTest {
        val budget = Budget(id = "b1", category = TransactionCategory.ENTERTAINMENT, monthlyLimit = Money.fromMajor(50.0), month = month)

        val progress = (useCase(listOf(budget), emptyList())(month).first() as NovaResult.Success).data.single()

        assertThat(progress.spent).isEqualTo(Money.ZERO)
        assertThat(progress.isOverLimit).isFalse()
    }

    @Test
    fun `budgets from other months are excluded`() = runTest {
        val otherMonthBudget = Budget(
            id = "b1",
            category = TransactionCategory.FOOD,
            monthlyLimit = Money.fromMajor(100.0),
            month = month.minusMonths(1)
        )

        val progress = (useCase(listOf(otherMonthBudget), emptyList())(month).first() as NovaResult.Success).data

        assertThat(progress).isEmpty()
    }
}
