package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.assistant.AssistantInsightEngine
import com.novafinance.core.domain.fake.FakeBudgetRepository
import com.novafinance.core.domain.fake.FakeDebtRepository
import com.novafinance.core.domain.fake.FakeFinancialSourceRepository
import com.novafinance.core.domain.fake.FakeGoalRepository
import com.novafinance.core.domain.fake.FakeTransactionRepository
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetDreamDashboardDataUseCaseTest {

    private val source = FinancialSource(
        id = "src-1",
        name = "Checking",
        type = FinancialSourceType.BANK_ACCOUNT,
        currentBalance = Money.fromMajor(2000.0),
        availableBalance = Money.fromMajor(2000.0),
        createdAt = Instant.now()
    )

    private fun buildUseCase(
        financialSourceRepository: FakeFinancialSourceRepository = FakeFinancialSourceRepository(listOf(source)),
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(),
        budgetRepository: FakeBudgetRepository = FakeBudgetRepository(),
        goalRepository: FakeGoalRepository = FakeGoalRepository(),
        debtRepository: FakeDebtRepository = FakeDebtRepository()
    ): GetDreamDashboardDataUseCase {
        val dispatcher = Dispatchers.Default
        val getDashboardSummary = GetDashboardSummaryUseCase(financialSourceRepository, transactionRepository, dispatcher)
        val getGoalForecast = GetGoalForecastUseCase(goalRepository, dispatcher)
        val getForecastSummary = GetForecastSummaryUseCase(getDashboardSummary, dispatcher)
        val getBalanceOverview = GetBalanceOverviewUseCase(financialSourceRepository, goalRepository, debtRepository, getForecastSummary, dispatcher)
        val getDebtSummary = GetDebtSummaryUseCase(debtRepository, getDashboardSummary, getBalanceOverview, dispatcher)
        return GetDreamDashboardDataUseCase(
            getDashboardSummary = getDashboardSummary,
            getForecastSummary = getForecastSummary,
            getBalanceOverview = getBalanceOverview,
            getGoalForecast = getGoalForecast,
            getAssistantContext = GetAssistantContextUseCase(
                getDashboardSummary = getDashboardSummary,
                getAnalyticsSummary = GetAnalyticsSummaryUseCase(transactionRepository, dispatcher),
                getBudgetProgress = GetBudgetProgressUseCase(budgetRepository, transactionRepository, dispatcher),
                getGoalForecast = getGoalForecast,
                getDebtSummary = getDebtSummary,
                dispatcher = dispatcher
            ),
            getDebtSummary = getDebtSummary,
            financialSourceRepository = financialSourceRepository,
            assistantInsightEngine = AssistantInsightEngine(),
            dispatcher = dispatcher
        )
    }

    @Test
    fun `combines every underlying use case into one success`() = runTest {
        val useCase = buildUseCase()

        val result = useCase(Unit).first()

        assertThat(result).isInstanceOf(NovaResult.Success::class.java)
        val data = (result as NovaResult.Success).data
        assertThat(data.summary.totalBalance).isEqualTo(Money.fromMajor(2000.0))
        assertThat(data.balanceOverview.totalLiquidity).isEqualTo(Money.fromMajor(2000.0))
    }

    @Test
    fun `goal forecasts match what GetGoalForecastUseCase would return on its own`() = runTest {
        val goal = SavingsGoal(
            id = "goal-1",
            name = "Wedding fund",
            targetAmount = Money.fromMajor(1000.0),
            currentAmount = Money.fromMajor(250.0),
            targetDate = null,
            createdAt = LocalDate.now()
        )
        val useCase = buildUseCase(goalRepository = FakeGoalRepository(listOf(goal)))

        val data = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(data.goalForecasts).hasSize(1)
        assertThat(data.goalForecasts.single().goal.id).isEqualTo("goal-1")
    }

    @Test
    fun `top insight comes from the real assistant engine, not a placeholder string`() = runTest {
        val transactions = listOf(
            Transaction(
                id = "txn-1",
                accountId = source.id,
                merchant = "Grocery",
                category = TransactionCategory.FOOD,
                amount = Money.fromMajor(-50.0),
                date = LocalDate.now()
            )
        )
        val useCase = buildUseCase(transactionRepository = FakeTransactionRepository(transactions))

        val data = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(data.topInsightMessage).isNotEmpty()
    }

    @Test
    fun `debt summary and top debt recommendation are present when active debt exists`() = runTest {
        val debt = Debt(
            id = "d1",
            name = "Credit card",
            direction = DebtDirection.I_OWE,
            type = DebtType.CREDIT_CARD,
            originalAmount = Money.fromMajor(2000.0),
            currentBalance = Money.fromMajor(800.0),
            minimumMonthlyPayment = Money.fromMajor(100.0),
            createdAt = LocalDate.now()
        )
        val useCase = buildUseCase(debtRepository = FakeDebtRepository(listOf(debt)))

        val data = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(data.debtSummary.totalOwed).isEqualTo(Money.fromMajor(800.0))
        assertThat(data.topDebtRecommendation).isNotNull()
    }

    @Test
    fun `no debts means no top debt recommendation`() = runTest {
        val useCase = buildUseCase()

        val data = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(data.topDebtRecommendation).isNull()
    }

    @Test
    fun `source healths are computed one per active source`() = runTest {
        val useCase = buildUseCase()

        val data = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(data.sourceHealths).hasSize(1)
        assertThat(data.sourceHealths.single().sourceId).isEqualTo(source.id)
    }
}
