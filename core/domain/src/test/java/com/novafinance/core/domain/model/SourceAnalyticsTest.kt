package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class SourceAnalyticsTest {

    private fun source(balance: Double, type: FinancialSourceType, creditLimit: Double? = null) = FinancialSource(
        id = "src-$type-$balance",
        name = type.displayName,
        type = type,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        creditLimit = creditLimit?.let { Money.fromMajor(it) },
        createdAt = Instant.now()
    )

    @Test
    fun `balance distribution groups by type and sums correctly`() {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(500.0, FinancialSourceType.BANK_ACCOUNT),
            source(2000.0, FinancialSourceType.SAVINGS_ACCOUNT)
        )

        val analytics = calculateSourceAnalytics(sources, emptyList(), emptyList(), emptyList())

        val bankEntry = analytics.balanceDistribution.single { it.type == FinancialSourceType.BANK_ACCOUNT }
        assertThat(bankEntry.balance).isEqualTo(Money.fromMajor(1500.0))
    }

    @Test
    fun `percentages across balance distribution sum to approximately 1`() {
        val sources = listOf(
            source(1000.0, FinancialSourceType.BANK_ACCOUNT),
            source(3000.0, FinancialSourceType.SAVINGS_ACCOUNT)
        )

        val analytics = calculateSourceAnalytics(sources, emptyList(), emptyList(), emptyList())

        val totalPercent = analytics.balanceDistribution.sumOf { it.percentOfTotal.toDouble() }
        assertThat(totalPercent).isWithin(0.01).of(1.0)
    }

    @Test
    fun `source allocation is sorted largest balance first`() {
        val sources = listOf(source(100.0, FinancialSourceType.CASH), source(5000.0, FinancialSourceType.BANK_ACCOUNT))

        val analytics = calculateSourceAnalytics(sources, emptyList(), emptyList(), emptyList())

        assertThat(analytics.sourceAllocation.first().balance).isEqualTo(Money.fromMajor(5000.0))
    }

    @Test
    fun `credit utilization only includes credit cards with a limit set, sorted highest first`() {
        val sources = listOf(
            source(900.0, FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0), // 90%
            source(100.0, FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0), // 10%
            source(500.0, FinancialSourceType.CREDIT_CARD, creditLimit = null), // no limit, excluded
            source(2000.0, FinancialSourceType.BANK_ACCOUNT) // not a card, excluded
        )

        val analytics = calculateSourceAnalytics(sources, emptyList(), emptyList(), emptyList())

        assertThat(analytics.creditUtilizations).hasSize(2)
        assertThat(analytics.creditUtilizations.first().utilization.utilizationPercent).isEqualTo(90)
    }

    @Test
    fun `no sources at all produces empty lists rather than crashing on a division by zero`() {
        val analytics = calculateSourceAnalytics(emptyList(), emptyList(), emptyList(), emptyList())

        assertThat(analytics.balanceDistribution).isEmpty()
        assertThat(analytics.sourceAllocation).isEmpty()
    }

    @Test
    fun `trend points are sorted chronologically regardless of input order`() {
        val now = Instant.now()
        val points = listOf(
            TrendPoint(now, Money.fromMajor(300.0)),
            TrendPoint(now.minusSeconds(3600), Money.fromMajor(100.0)),
            TrendPoint(now.minusSeconds(1800), Money.fromMajor(200.0))
        )

        val analytics = calculateSourceAnalytics(emptyList(), emptyList(), points, emptyList())

        assertThat(analytics.liquidityTrend.map { it.amount }).isEqualTo(listOf(Money.fromMajor(100.0), Money.fromMajor(200.0), Money.fromMajor(300.0)))
    }

    @Test
    fun `empty snapshot history produces an empty trend, not an error`() {
        val analytics = calculateSourceAnalytics(emptyList(), emptyList(), emptyList(), emptyList())

        assertThat(analytics.liquidityTrend).isEmpty()
        assertThat(analytics.savingsGrowthTrend).isEmpty()
    }

    @Test
    fun `credit utilization respects reconciliation when a card is linked to a debt`() {
        val linkedCard = source(100.0, FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0).copy(linkedDebtId = "d1")
        val debts = listOf(
            Debt(
                id = "d1", name = "Card", direction = DebtDirection.I_OWE, type = DebtType.CREDIT_CARD,
                originalAmount = Money.fromMajor(1000.0), currentBalance = Money.fromMajor(700.0),
                createdAt = java.time.LocalDate.now()
            )
        )

        val analytics = calculateSourceAnalytics(listOf(linkedCard), debts, emptyList(), emptyList())

        assertThat(analytics.creditUtilizations.single().utilization.utilizationPercent).isEqualTo(70)
    }
}
