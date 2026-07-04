package com.novafinance.core.domain.usecase

import com.google.common.truth.Truth.assertThat
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

class GetFinancialSourceIntelligenceUseCaseTest {

    private fun source(id: String, balance: Double, type: FinancialSourceType = FinancialSourceType.BANK_ACCOUNT, creditLimit: Double? = null) = FinancialSource(
        id = id,
        name = id,
        type = type,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        creditLimit = creditLimit?.let { Money.fromMajor(it) },
        createdAt = Instant.now()
    )

    private fun buildUseCase(sources: List<FinancialSource>): GetFinancialSourceIntelligenceUseCase {
        val dispatcher = Dispatchers.Default
        val financialSourceRepository = FakeFinancialSourceRepository(sources)
        val transactionRepository = FakeTransactionRepository()
        val goalRepository = FakeGoalRepository()
        val getDashboardSummary = GetDashboardSummaryUseCase(financialSourceRepository, transactionRepository, dispatcher)
        val getForecastSummary = GetForecastSummaryUseCase(getDashboardSummary, dispatcher)
        val debtRepository = FakeDebtRepository()
        val getBalanceOverview = GetBalanceOverviewUseCase(financialSourceRepository, goalRepository, debtRepository, getForecastSummary, dispatcher)
        val getDebtSummary = GetDebtSummaryUseCase(debtRepository, getDashboardSummary, getBalanceOverview, dispatcher)
        return GetFinancialSourceIntelligenceUseCase(
            financialSourceRepository = financialSourceRepository,
            transactionRepository = transactionRepository,
            getBalanceOverview = getBalanceOverview,
            getDashboardSummary = getDashboardSummary,
            getDebtSummary = getDebtSummary,
            getForecastSummary = getForecastSummary,
            getGoalForecast = GetGoalForecastUseCase(goalRepository, dispatcher),
            dispatcher = dispatcher
        )
    }

    @Test
    fun `produces one source health and one source forecast per active source`() = runTest {
        val sources = listOf(source("s1", 1000.0), source("s2", 2000.0))
        val useCase = buildUseCase(sources)

        val result = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(result.sourceHealths).hasSize(2)
        assertThat(result.sourceForecasts).hasSize(2)
        assertThat(result.healthFor("s1")).isNotNull()
        assertThat(result.forecastFor("s2")).isNotNull()
    }

    @Test
    fun `inactive sources are excluded from per-source health and forecast`() = runTest {
        val sources = listOf(
            source("active", 1000.0),
            source("inactive", 500.0).copy(isActive = false)
        )
        val useCase = buildUseCase(sources)

        val result = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(result.sourceHealths.map { it.sourceId }).containsExactly("active")
        assertThat(result.healthFor("inactive")).isNull()
    }

    @Test
    fun `balance health is computed and within range`() = runTest {
        val useCase = buildUseCase(listOf(source("s1", 5000.0)))

        val result = (useCase(Unit).first() as NovaResult.Success).data

        assertThat(result.balanceHealth.score).isIn(0..100)
    }

    @Test
    fun `no sources at all still succeeds with empty lists rather than an error`() = runTest {
        val useCase = buildUseCase(emptyList())

        val result = useCase(Unit).first()

        assertThat(result).isInstanceOf(NovaResult.Success::class.java)
        val data = (result as NovaResult.Success).data
        assertThat(data.sourceHealths).isEmpty()
        assertThat(data.sourceForecasts).isEmpty()
    }
}
