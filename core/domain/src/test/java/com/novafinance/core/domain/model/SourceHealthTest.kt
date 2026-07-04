package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class SourceHealthTest {

    private fun source(
        balance: Double,
        type: FinancialSourceType = FinancialSourceType.BANK_ACCOUNT,
        creditLimit: Double? = null
    ) = FinancialSource(
        id = "src-1",
        name = "Test source",
        type = type,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        creditLimit = creditLimit?.let { Money.fromMajor(it) },
        createdAt = Instant.now()
    )

    @Test
    fun `a credit card is scored purely on utilization regardless of expense data`() {
        val healthyCard = source(balance = 100.0, type = FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0)

        val health = calculateSourceHealth(healthyCard, monthlyExpense = Money.ZERO)

        assertThat(health.score).isEqualTo(90) // 100 - 10% utilization
        assertThat(health.label).isEqualTo(DebtHealthLabel.HEALTHY)
    }

    @Test
    fun `a maxed out credit card scores critically`() {
        val maxedCard = source(balance = 950.0, type = FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0)

        val health = calculateSourceHealth(maxedCard, monthlyExpense = Money.fromMajor(500.0))

        assertThat(health.label).isEqualTo(DebtHealthLabel.CRITICAL)
    }

    @Test
    fun `a non credit card source with six or more months of expense buffer scores at the top`() {
        val wellBuffered = source(balance = 6000.0)

        val health = calculateSourceHealth(wellBuffered, monthlyExpense = Money.fromMajor(1000.0))

        assertThat(health.score).isEqualTo(100)
        assertThat(health.label).isEqualTo(DebtHealthLabel.HEALTHY)
    }

    @Test
    fun `a non credit card source with no buffer at all scores at the bottom`() {
        val noBuffer = source(balance = 0.0)

        val health = calculateSourceHealth(noBuffer, monthlyExpense = Money.fromMajor(1000.0))

        assertThat(health.score).isEqualTo(0)
    }

    @Test
    fun `missing expense data with a positive balance is a neutral pass, not a zero`() {
        val health = calculateSourceHealth(source(balance = 500.0), monthlyExpense = Money.ZERO)

        assertThat(health.score).isGreaterThan(0)
    }

    @Test
    fun `score is always within 0 to 100`() {
        val extreme = source(balance = 1_000_000.0)

        val health = calculateSourceHealth(extreme, monthlyExpense = Money.fromMajor(0.01))

        assertThat(health.score).isIn(0..100)
    }
}
