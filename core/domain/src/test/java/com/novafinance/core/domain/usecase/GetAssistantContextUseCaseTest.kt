package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.fake.FakeBudgetRepository
import com.novafinance.core.domain.fake.FakeDebtRepository
import com.novafinance.core.domain.fake.FakeFinancialSourceRepository
import com.novafinance.core.domain.fake.FakeGoalRepository
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetAssistantContextUseCaseTest {

    private fun buildUseCase(
        financialSourceRepository: FakeFinancialSourceRepository = FakeFinancialSourceRepository(),
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        budgetRepository: FakeBudgetRepository = FakeBudgetRepository(),
        goalRepository: FakeGoalRepository = FakeGoalRepository(),
        debtRepository: FakeDebtRepository = FakeDebtRepository()
    ): GetAssistantContextUseCase {
        val dispatcher = Dispatchers.Default
        val getDashboardSummary = GetDashboardSummaryUseCase(financialSourceRepository, transactionRepository, dispatcher)
        return GetAssistantContextUseCase(
            getDashboardSummary = getDashboardSummary,
            getAnalyticsSummary = GetAnalyticsSummaryUseCase(transactionRepository, dispatcher),
            getBudgetProgress = GetBudgetProgressUseCase(budgetRepository, transactionRepository, dispatcher),
            getGoalForecast = GetGoalForecastUseCase(goalRepository, dispatcher),
            getDebtSummary = GetDebtSummaryUseCase(
                debtRepository = debtRepository,
                getDashboardSummary = getDashboardSummary,
                getBalanceOverview = GetBalanceOverviewUseCase(
                    financialSourceRepository = financialSourceRepository,
                    goalRepository = goalRepository,
                    debtRepository = debtRepository,
                    getForecastSummary = GetForecastSummaryUseCase(getDashboardSummary, dispatcher),
                    dispatcher = dispatcher
                ),
                dispatcher = dispatcher
            ),
            dispatcher = dispatcher
        )
    }

    @Test
    fun `succeeds and combines every source when all five underlying use cases succeed`() = runTest {
        val source = FinancialSource(
            id = "src-1",
            name = "Checking",
            type = FinancialSourceType.BANK_ACCOUNT,
            currentBalance = Money.fromMajor(1000.0),
            availableBalance = Money.fromMajor(1000.0),
            createdAt = Instant.now()
        )
        val useCase = buildUseCase(financialSourceRepository = FakeFinancialSourceRepository(listOf(source)))

        val result = useCase(Unit).first()

        assertThat(result).isInstanceOf(NovaResult.Success::class.java)
        val context = (result as NovaResult.Success).data
        assertThat(context.dashboard.totalBalance).isEqualTo(Money.fromMajor(1000.0))
        assertThat(context.analytics.monthlyTrend).hasSize(6)
        assertThat(context.budgetProgress).isEmpty()
        assertThat(context.goalForecasts).isEmpty()
        assertThat(context.debtSummary.health.score).isEqualTo(100)
    }

    @Test
    fun `hasActivity is false when there are no transactions to reason about`() = runTest {
        val useCase = buildUseCase()

        val context = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(context.hasActivity).isFalse()
    }
}
