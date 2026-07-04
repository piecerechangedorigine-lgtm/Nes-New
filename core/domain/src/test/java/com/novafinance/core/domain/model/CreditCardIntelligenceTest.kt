package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test

class CreditCardIntelligenceTest {

    @Test
    fun `low utilization is healthy`() {
        val result = calculateCreditCardUtilization(usedAmount = Money.fromMajor(200.0), creditLimit = Money.fromMajor(2000.0))

        assertThat(result.utilizationPercent).isEqualTo(10)
        assertThat(result.label).isEqualTo(CreditUtilizationLabel.HEALTHY)
    }

    @Test
    fun `utilization bands match standard credit guidance thresholds`() {
        assertThat(calculateCreditCardUtilization(Money.fromMajor(29.0), Money.fromMajor(100.0)).label).isEqualTo(CreditUtilizationLabel.HEALTHY)
        assertThat(calculateCreditCardUtilization(Money.fromMajor(30.0), Money.fromMajor(100.0)).label).isEqualTo(CreditUtilizationLabel.MODERATE)
        assertThat(calculateCreditCardUtilization(Money.fromMajor(50.0), Money.fromMajor(100.0)).label).isEqualTo(CreditUtilizationLabel.HIGH_UTILIZATION)
        assertThat(calculateCreditCardUtilization(Money.fromMajor(80.0), Money.fromMajor(100.0)).label).isEqualTo(CreditUtilizationLabel.CRITICAL)
    }

    @Test
    fun `available credit floors at zero rather than going negative`() {
        val result = calculateCreditCardUtilization(usedAmount = Money.fromMajor(1200.0), creditLimit = Money.fromMajor(1000.0))

        assertThat(result.availableCredit).isEqualTo(Money.ZERO)
    }

    @Test
    fun `utilization can exceed 100 percent when a card is over its limit, not hidden or clamped`() {
        val result = calculateCreditCardUtilization(usedAmount = Money.fromMajor(1200.0), creditLimit = Money.fromMajor(1000.0))

        assertThat(result.utilizationPercent).isEqualTo(120)
        assertThat(result.label).isEqualTo(CreditUtilizationLabel.CRITICAL)
    }

    @Test
    fun `a zero credit limit does not crash with a division by zero`() {
        val result = calculateCreditCardUtilization(usedAmount = Money.fromMajor(50.0), creditLimit = Money.ZERO)

        assertThat(result.utilizationPercent).isEqualTo(0)
    }

    @Test
    fun `FinancialSource creditCardUtilization is null for non credit card types even with a limit set`() {
        val source = FinancialSource(
            id = "src-1",
            name = "Checking",
            type = FinancialSourceType.BANK_ACCOUNT,
            currentBalance = Money.fromMajor(100.0),
            availableBalance = Money.fromMajor(100.0),
            creditLimit = Money.fromMajor(1000.0),
            createdAt = Instant.now()
        )

        assertThat(source.creditCardUtilization).isNull()
    }

    @Test
    fun `FinancialSource creditCardUtilization is null for a credit card with no limit set`() {
        val source = FinancialSource(
            id = "src-1",
            name = "Card",
            type = FinancialSourceType.CREDIT_CARD,
            currentBalance = Money.fromMajor(100.0),
            availableBalance = Money.fromMajor(100.0),
            creditLimit = null,
            createdAt = Instant.now()
        )

        assertThat(source.creditCardUtilization).isNull()
    }

    @Test
    fun `FinancialSource creditCardUtilization is computed for a credit card with a limit set`() {
        val source = FinancialSource(
            id = "src-1",
            name = "Card",
            type = FinancialSourceType.CREDIT_CARD,
            currentBalance = Money.fromMajor(300.0),
            availableBalance = Money.fromMajor(300.0),
            creditLimit = Money.fromMajor(1000.0),
            createdAt = Instant.now()
        )

        assertThat(source.creditCardUtilization).isNotNull()
        assertThat(source.creditCardUtilization!!.utilizationPercent).isEqualTo(30)
    }
}
