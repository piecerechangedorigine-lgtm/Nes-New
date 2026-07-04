package com.novafinance.core.domain.assistant

import com.google.common.truth.Truth.assertThat
import com.novafinance.core.domain.model.AnalyticsSummary
import com.novafinance.core.domain.model.AssistantActionType
import com.novafinance.core.domain.model.AssistantContext
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.model.BudgetProgress
import com.novafinance.core.domain.model.CategoryBreakdown
import com.novafinance.core.domain.model.DashboardInsight
import com.novafinance.core.domain.model.DashboardSummary
import com.novafinance.core.domain.model.DebtHealthLabel
import com.novafinance.core.domain.model.DebtHealthScore
import com.novafinance.core.domain.model.DebtPressureLabel
import com.novafinance.core.domain.model.DebtPressureScore
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.DebtTrend
import com.novafinance.core.domain.model.DebtWeather
import com.novafinance.core.domain.model.DebtWeatherState
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.InsightTone
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.PayoffPlanResult
import com.novafinance.core.domain.model.PayoffStrategy
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Test

class AssistantInsightEngineTest {

    private val engine = AssistantInsightEngine()

    private val sampleTransaction = Transaction(
        id = "txn-1",
        accountId = "acc-1",
        merchant = "Grocery",
        category = TransactionCategory.FOOD,
        amount = Money.fromMajor(-50.0),
        date = LocalDate.now()
    )

    private val emptyDebtSummary = DebtSummary(
        debts = emptyList(),
        totalOwed = Money.ZERO,
        totalOwedToMe = Money.ZERO,
        health = DebtHealthScore(100, DebtHealthLabel.HEALTHY, "No active debt."),
        weather = DebtWeather(DebtWeatherState.SUNNY, DebtTrend.STABLE),
        pressure = DebtPressureScore(0, DebtPressureLabel.LOW),
        freedomProjection = PayoffPlanResult(PayoffStrategy.FASTEST_FREEDOM, LocalDate.now(), 0, Money.ZERO, emptyList())
    )

    private fun context(
        recentTransactions: List<Transaction> = listOf(sampleTransaction),
        totalBalance: Money = Money.fromMajor(1000.0),
        monthIncome: Money = Money.fromMajor(2000.0),
        monthExpense: Money = Money.fromMajor(500.0),
        dashboardInsight: DashboardInsight? = null,
        breakdown: List<CategoryBreakdown> = emptyList(),
        monthOverMonthChange: Float? = null,
        budgetProgress: List<BudgetProgress> = emptyList(),
        goalForecasts: List<GoalForecast> = emptyList(),
        debtSummary: DebtSummary = emptyDebtSummary
    ) = AssistantContext(
        dashboard = DashboardSummary(
            totalBalance = totalBalance,
            monthIncome = monthIncome,
            monthExpense = monthExpense,
            recentTransactions = recentTransactions,
            insight = dashboardInsight
        ),
        analytics = AnalyticsSummary(
            currentMonthBreakdown = breakdown,
            monthlyTrend = emptyList(),
            monthOverMonthExpenseChangePercent = monthOverMonthChange
        ),
        budgetProgress = budgetProgress,
        goalForecasts = goalForecasts,
        debtSummary = debtSummary
    )

    @Test
    fun `no activity yet asks the person to add a transaction instead of answering`() {
        val reply = engine.respond("how's my spending?", context(recentTransactions = emptyList()))

        assertThat(reply.text).contains("don't have any transactions")
        assertThat(reply.actions.single().type).isEqualTo(AssistantActionType.ADD_TRANSACTION)
    }

    @Test
    fun `suggested prompts are minimal with no activity and richer once there's real data`() {
        assertThat(engine.suggestedPrompts(context(recentTransactions = emptyList()))).hasSize(1)

        val fullContext = context(
            budgetProgress = listOf(
                BudgetProgress(
                    budget = Budget("b1", TransactionCategory.FOOD, Money.fromMajor(300.0), YearMonth.now()),
                    spent = Money.fromMajor(100.0)
                )
            ),
            goalForecasts = listOf(
                GoalForecast(
                    goal = SavingsGoal("g1", "Trip", Money.fromMajor(1000.0), Money.ZERO, null, LocalDate.now()),
                    requiredMonthlyContribution = null,
                    monthsRemaining = null
                )
            )
        )
        assertThat(engine.suggestedPrompts(fullContext)).hasSize(4)
    }

    @Test
    fun `budget query with no budgets set up suggests creating one`() {
        val reply = engine.respond("am I over budget?", context(budgetProgress = emptyList()))

        assertThat(reply.text).contains("don't have any budgets")
        assertThat(reply.actions.single().type).isEqualTo(AssistantActionType.OPEN_BUDGETS)
    }

    @Test
    fun `budget query names every category that is over its limit`() {
        val overLimit = BudgetProgress(
            budget = Budget("b1", TransactionCategory.FOOD, Money.fromMajor(200.0), YearMonth.now()),
            spent = Money.fromMajor(250.0)
        )
        val withinLimit = BudgetProgress(
            budget = Budget("b2", TransactionCategory.TRANSPORT, Money.fromMajor(100.0), YearMonth.now()),
            spent = Money.fromMajor(20.0)
        )

        val reply = engine.respond("budget check", context(budgetProgress = listOf(overLimit, withinLimit)))

        assertThat(reply.text).contains(TransactionCategory.FOOD.displayName)
        assertThat(reply.text).doesNotContain(TransactionCategory.TRANSPORT.displayName)
    }

    @Test
    fun `budget query reports all clear when every budget is within limit`() {
        val withinLimit = BudgetProgress(
            budget = Budget("b1", TransactionCategory.FOOD, Money.fromMajor(200.0), YearMonth.now()),
            spent = Money.fromMajor(50.0)
        )

        val reply = engine.respond("budget check", context(budgetProgress = listOf(withinLimit)))

        assertThat(reply.text).contains("within limit")
    }

    @Test
    fun `goal query with no goals suggests creating one`() {
        val reply = engine.respond("am I saving enough?", context(goalForecasts = emptyList()))

        assertThat(reply.text).contains("don't have any savings goals")
        assertThat(reply.actions.single().type).isEqualTo(AssistantActionType.OPEN_GOALS)
    }

    @Test
    fun `goal query surfaces the real required monthly contribution`() {
        val forecast = GoalForecast(
            goal = SavingsGoal("g1", "Wedding fund", Money.fromMajor(6000.0), Money.fromMajor(1000.0), LocalDate.now().plusMonths(5), LocalDate.now()),
            requiredMonthlyContribution = Money.fromMajor(1000.0),
            monthsRemaining = 5
        )

        val reply = engine.respond("goal progress", context(goalForecasts = listOf(forecast)))

        assertThat(reply.text).contains("Wedding fund")
        assertThat(reply.text).contains(Money.fromMajor(1000.0).formatted())
        assertThat(reply.text).contains("5")
    }

    @Test
    fun `spending query names the biggest category and its share of spend`() {
        val breakdown = listOf(
            CategoryBreakdown(TransactionCategory.FOOD, Money.fromMajor(300.0), 0.75f),
            CategoryBreakdown(TransactionCategory.TRANSPORT, Money.fromMajor(100.0), 0.25f)
        )

        val reply = engine.respond("how's my spending", context(breakdown = breakdown))

        assertThat(reply.text).contains(TransactionCategory.FOOD.displayName)
        assertThat(reply.text).contains("75%")
        assertThat(reply.actions.single().type).isEqualTo(AssistantActionType.OPEN_ANALYTICS)
    }

    @Test
    fun `balance query reports the real total balance income and expense`() {
        val reply = engine.respond(
            "what's my balance",
            context(totalBalance = Money.fromMajor(4200.0), monthIncome = Money.fromMajor(3000.0), monthExpense = Money.fromMajor(1200.0))
        )

        assertThat(reply.text).contains(Money.fromMajor(4200.0).formatted())
        assertThat(reply.text).contains(Money.fromMajor(3000.0).formatted())
        assertThat(reply.text).contains(Money.fromMajor(1200.0).formatted())
    }

    @Test
    fun `unrecognized query falls back to the dashboard insight when one exists`() {
        val insight = DashboardInsight("You're on track to save over 20% of your income this month.", InsightTone.POSITIVE)

        val reply = engine.respond("blah completely unrelated", context(dashboardInsight = insight))

        assertThat(reply.text).contains(insight.message)
    }

    @Test
    fun `unrecognized query with no dashboard insight gives a generic pointer rather than making something up`() {
        val reply = engine.respond("blah completely unrelated", context(dashboardInsight = null))

        assertThat(reply.text).contains("spending, budgets, and savings goals")
    }

    @Test
    fun `debt query with no debts tracked suggests opening the debt center`() {
        val reply = engine.respond("how's my debt looking", context(debtSummary = emptyDebtSummary))

        assertThat(reply.text).contains("don't have any debts tracked")
        assertThat(reply.actions.single().type).isEqualTo(AssistantActionType.OPEN_DEBT)
    }

    @Test
    fun `debt query with active debt reports the real health score and freedom date`() {
        val sampleDebt = com.novafinance.core.domain.model.Debt(
            id = "d1",
            name = "Car loan",
            direction = com.novafinance.core.domain.model.DebtDirection.I_OWE,
            type = com.novafinance.core.domain.model.DebtType.CAR_LOAN,
            originalAmount = Money.fromMajor(10000.0),
            currentBalance = Money.fromMajor(4000.0),
            minimumMonthlyPayment = Money.fromMajor(250.0),
            createdAt = LocalDate.now()
        )
        val debtSummary = emptyDebtSummary.copy(
            debts = listOf(sampleDebt),
            totalOwed = Money.fromMajor(4000.0),
            health = DebtHealthScore(72, DebtHealthLabel.MODERATE, "manageable"),
            freedomProjection = PayoffPlanResult(PayoffStrategy.FASTEST_FREEDOM, LocalDate.of(2027, 6, 1), 18, Money.ZERO, listOf("d1"))
        )

        val reply = engine.respond("how much debt do I owe", context(debtSummary = debtSummary))

        assertThat(reply.text).contains("moderate")
        assertThat(reply.text).contains("72/100")
        assertThat(reply.text).contains("2027-06-01")
    }

    @Test
    fun `topDebtRecommendation returns null when there is no active owed debt`() {
        assertThat(engine.topDebtRecommendation(context(debtSummary = emptyDebtSummary))).isNull()
    }
}
