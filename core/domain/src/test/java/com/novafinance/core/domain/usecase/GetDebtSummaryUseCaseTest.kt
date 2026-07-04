package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
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
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetDebtSummaryUseCaseTest {

    private val source = FinancialSource(
        id = "src-1",
        name = "Checking",
        type = FinancialSourceType.BANK_ACCOUNT,
        currentBalance = Money.fromMajor(5000.0),
        availableBalance = Money.fromMajor(5000.0),
        createdAt = Instant.now()
    )

    private fun buildUseCase(
        debtRepository: FakeDebtRepository = FakeDebtRepository(),
        financialSourceRepository: FakeFinancialSourceRepository = FakeFinancialSourceRepository(listOf(source))
    ): GetDebtSummaryUseCase {
        val dispatcher = Dispatchers.Default
        val transactionRepository = FakeTransactionRepository()
        val getDashboardSummary = GetDashboardSummaryUseCase(financialSourceRepository, transactionRepository, dispatcher)
        return GetDebtSummaryUseCase(
            debtRepository = debtRepository,
            getDashboardSummary = getDashboardSummary,
            getBalanceOverview = GetBalanceOverviewUseCase(
                financialSourceRepository = financialSourceRepository,
                goalRepository = FakeGoalRepository(),
                debtRepository = debtRepository,
                getForecastSummary = GetForecastSummaryUseCase(getDashboardSummary, dispatcher),
                dispatcher = dispatcher
            ),
            dispatcher = dispatcher
        )
    }

    @Test
    fun `no debts is a healthy summary with zero totals`() = runTest {
        val useCase = buildUseCase()

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.totalOwed).isEqualTo(Money.ZERO)
        assertThat(summary.totalOwedToMe).isEqualTo(Money.ZERO)
        assertThat(summary.health.score).isEqualTo(100)
    }

    @Test
    fun `total owed and total owed to me are summed separately by direction`() = runTest {
        val debts = listOf(
            Debt("d1", "Loan", DebtDirection.I_OWE, DebtType.PERSONAL_LOAN, Money.fromMajor(1000.0), Money.fromMajor(600.0), createdAt = LocalDate.now()),
            Debt("d2", "Card", DebtDirection.I_OWE, DebtType.CREDIT_CARD, Money.fromMajor(500.0), Money.fromMajor(200.0), createdAt = LocalDate.now()),
            Debt("d3", "Friend", DebtDirection.OWED_TO_ME, DebtType.FAMILY_OR_FRIEND, Money.fromMajor(300.0), Money.fromMajor(300.0), createdAt = LocalDate.now())
        )
        val useCase = buildUseCase(debtRepository = FakeDebtRepository(debts))

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.totalOwed).isEqualTo(Money.fromMajor(800.0))
        assertThat(summary.totalOwedToMe).isEqualTo(Money.fromMajor(300.0))
    }

    @Test
    fun `debts list in the summary matches the repository`() = runTest {
        val debts = listOf(
            Debt("d1", "Loan", DebtDirection.I_OWE, DebtType.PERSONAL_LOAN, Money.fromMajor(1000.0), Money.fromMajor(600.0), createdAt = LocalDate.now())
        )
        val useCase = buildUseCase(debtRepository = FakeDebtRepository(debts))

        val summary = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(summary.debts).hasSize(1)
        assertThat(summary.activeOwedDebts).hasSize(1)
        assertThat(summary.activeReceivables).isEmpty()
    }
}
