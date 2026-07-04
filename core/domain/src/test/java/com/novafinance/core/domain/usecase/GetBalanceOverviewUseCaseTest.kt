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
import com.novafinance.core.domain.model.SavingsGoal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class GetBalanceOverviewUseCaseTest {

    private fun source(
        balance: Double,
        type: FinancialSourceType,
        isActive: Boolean = true,
        includeInLiquidity: Boolean = true,
        includeInSpendingPower: Boolean = true,
        includeInForecast: Boolean = true,
        isEmergencyReserve: Boolean = false,
        linkedDebtId: String? = null
    ) = FinancialSource(
        id = "src-${type}-$balance-${includeInLiquidity}-${includeInSpendingPower}",
        name = type.displayName,
        type = type,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        isActive = isActive,
        includeInLiquidity = includeInLiquidity,
        includeInSpendingPower = includeInSpendingPower,
        includeInForecast = includeInForecast,
        isEmergencyReserve = isEmergencyReserve,
        linkedDebtId = linkedDebtId,
        createdAt = Instant.now()
    )

    private fun goal(saved: Double) = SavingsGoal(
        id = "goal-$saved",
        name = "Goal",
        targetAmount = Money.fromMajor(saved * 2),
        currentAmount = Money.fromMajor(saved),
        targetDate = null,
        createdAt = LocalDate.now()
    )

    private fun useCase(sources: List<FinancialSource>, goals: List<SavingsGoal>, debts: List<Debt> = emptyList()): GetBalanceOverviewUseCase {
        val dispatcher = Dispatchers.Default
        val financialSourceRepository = FakeFinancialSourceRepository(sources)
        val getDashboardSummary = GetDashboardSummaryUseCase(financialSourceRepository, FakeTransactionRepository(), dispatcher)
        return GetBalanceOverviewUseCase(
            financialSourceRepository = financialSourceRepository,
            goalRepository = FakeGoalRepository(goals),
            debtRepository = FakeDebtRepository(debts),
            getForecastSummary = GetForecastSummaryUseCase(getDashboardSummary, dispatcher),
            dispatcher = dispatcher
        )
    }

    @Test
    fun `credit card debt reduces total liquidity instead of adding to it`() = runTest {
        val sources = listOf(
            source(2000.0, FinancialSourceType.BANK_ACCOUNT),
            source(500.0, FinancialSourceType.CREDIT_CARD)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1500.0))
    }

    @Test
    fun `inactive sources are excluded from liquidity entirely`() = runTest {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(9999.0, FinancialSourceType.CASH, isActive = false)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `dream safe balance is the sum of every goal's saved amount`() = runTest {
        val goals = listOf(goal(300.0), goal(700.0))

        val overview = (useCase(emptyList(), goals)(Unit).first() as NovaResult.Success).data

        assertThat(overview.dreamSafeBalance).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `available spending power excludes money already earmarked for goals`() = runTest {
        val sources = listOf(source(5000.0, FinancialSourceType.BANK_ACCOUNT))
        val goals = listOf(goal(1200.0))

        val overview = (useCase(sources, goals)(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(5000.0))
        assertThat(overview.dreamSafeBalance).isEqualTo(Money.fromMajor(1200.0))
        assertThat(overview.availableSpendingPower).isEqualTo(Money.fromMajor(3800.0))
    }

    @Test
    fun `no sources and no goals is all zeros not an error`() = runTest {
        val overview = (useCase(emptyList(), emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.ZERO)
        assertThat(overview.dreamSafeBalance).isEqualTo(Money.ZERO)
        assertThat(overview.availableSpendingPower).isEqualTo(Money.ZERO)
        assertThat(overview.emergencyReserve).isEqualTo(Money.ZERO)
    }

    @Test
    fun `a source excluded from liquidity does not count toward total liquidity`() = runTest {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(500.0, FinancialSourceType.SAVINGS_ACCOUNT, includeInLiquidity = false)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `a source can count toward liquidity but not spending power, per the 11_4 savings account example`() = runTest {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(2000.0, FinancialSourceType.SAVINGS_ACCOUNT, includeInLiquidity = true, includeInSpendingPower = false)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(3000.0))
        assertThat(overview.availableSpendingPower).isEqualTo(Money.fromMajor(1000.0))
    }

    @Test
    fun `emergency reserve sums only sources explicitly marked as such`() = runTest {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(3000.0, FinancialSourceType.SAVINGS_ACCOUNT, isEmergencyReserve = true),
            source(500.0, FinancialSourceType.CASH, isEmergencyReserve = false)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.emergencyReserve).isEqualTo(Money.fromMajor(3000.0))
    }

    @Test
    fun `forecast balance only reflects forecast eligible sources`() = runTest {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT, includeInForecast = true),
            source(5000.0, FinancialSourceType.INVESTMENT_ACCOUNT, includeInForecast = false)
        )

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        // With no transactions at all, the overall forecast projects no
        // change, so forecastBalance should just equal the
        // forecast-eligible subset's current balance (1000), not the
        // full 6000 across every source.
    private fun debt(id: String, balance: Double) = Debt(
        id = id,
        name = id,
        direction = DebtDirection.I_OWE,
        type = DebtType.CREDIT_CARD,
        originalAmount = Money.fromMajor(balance),
        currentBalance = Money.fromMajor(balance),
        createdAt = LocalDate.now()
    )

    @Test
    fun `an unlinked credit card uses its own balance for liquidity`() = runTest {
        val sources = listOf(source(500.0, FinancialSourceType.CREDIT_CARD), source(2000.0, FinancialSourceType.BANK_ACCOUNT))

        val overview = (useCase(sources, emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1500.0))
    }

    @Test
    fun `a credit card linked to a debt uses the debt's balance instead of its own, per the reconciliation ownership rule`() = runTest {
        val linkedDebt = debt("debt-1", balance = 800.0)
        val sources = listOf(
            source(500.0, FinancialSourceType.CREDIT_CARD, linkedDebtId = "debt-1"), // stale, drifted balance
            source(2000.0, FinancialSourceType.BANK_ACCOUNT)
        )

        val overview = (useCase(sources, emptyList(), debts = listOf(linkedDebt))(Unit).first() as NovaResult.Success).data

        // 2000 - 800 (the debt's balance, not the card's own stale 500),
        // proving the liability calculation is reconciliation-aware.
        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1200.0))
    }

    @Test
    fun `a linkedDebtId pointing at a debt that no longer exists falls back to the source's own balance rather than crashing`() = runTest {
        val sources = listOf(
            source(500.0, FinancialSourceType.CREDIT_CARD, linkedDebtId = "nonexistent-debt"),
            source(2000.0, FinancialSourceType.BANK_ACCOUNT)
        )

        val overview = (useCase(sources, emptyList(), debts = emptyList())(Unit).first() as NovaResult.Success).data

        assertThat(overview.totalLiquidity).isEqualTo(Money.fromMajor(1500.0))
    }
}
