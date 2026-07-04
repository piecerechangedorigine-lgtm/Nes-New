package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class ReconciliationEngineTest {

    private fun source(balance: Double, linkedDebtId: String? = null, creditLimit: Double? = null) = FinancialSource(
        id = "src-1",
        name = "Card",
        type = FinancialSourceType.CREDIT_CARD,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        creditLimit = creditLimit?.let { Money.fromMajor(it) },
        linkedDebtId = linkedDebtId,
        createdAt = Instant.now()
    )

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
    fun `effectiveLiabilityBalance uses the source's own balance when unlinked`() {
        val unlinked = source(balance = 500.0)

        assertThat(effectiveLiabilityBalance(unlinked, emptyList())).isEqualTo(Money.fromMajor(500.0))
    }

    @Test
    fun `effectiveLiabilityBalance uses the linked debt's balance when linked`() {
        val linked = source(balance = 500.0, linkedDebtId = "d1")
        val debts = listOf(debt("d1", balance = 800.0))

        assertThat(effectiveLiabilityBalance(linked, debts)).isEqualTo(Money.fromMajor(800.0))
    }

    @Test
    fun `effectiveLiabilityBalance falls back to the source's own balance when the linked debt id does not resolve`() {
        val danglingLink = source(balance = 500.0, linkedDebtId = "does-not-exist")

        assertThat(effectiveLiabilityBalance(danglingLink, emptyList())).isEqualTo(Money.fromMajor(500.0))
    }

    @Test
    fun `effectiveCreditCardUtilization uses the reconciled balance, not the source's own`() {
        val linked = source(balance = 100.0, linkedDebtId = "d1", creditLimit = 1000.0)
        val debts = listOf(debt("d1", balance = 900.0))

        val utilization = effectiveCreditCardUtilization(linked, debts)

        assertThat(utilization).isNotNull()
        assertThat(utilization!!.utilizationPercent).isEqualTo(90) // from the debt's 900, not the source's own 100
    }

    @Test
    fun `effectiveCreditCardUtilization is null for a non credit card type regardless of linking`() {
        val nonCard = FinancialSource(
            id = "src-2",
            name = "Checking",
            type = FinancialSourceType.BANK_ACCOUNT,
            currentBalance = Money.fromMajor(100.0),
            availableBalance = Money.fromMajor(100.0),
            creditLimit = Money.fromMajor(1000.0),
            linkedDebtId = "d1",
            createdAt = Instant.now()
        )

        assertThat(effectiveCreditCardUtilization(nonCard, listOf(debt("d1", 900.0)))).isNull()
    }

    @Test
    fun `detectReconciliationConflicts finds nothing for an unlinked source`() {
        val unlinked = source(balance = 500.0)

        assertThat(detectReconciliationConflicts(listOf(unlinked), emptyList())).isEmpty()
    }

    @Test
    fun `detectReconciliationConflicts finds nothing when linked balances agree`() {
        val linked = source(balance = 500.0, linkedDebtId = "d1")
        val debts = listOf(debt("d1", balance = 500.0))

        assertThat(detectReconciliationConflicts(listOf(linked), debts)).isEmpty()
    }

    @Test
    fun `detectReconciliationConflicts ignores a trivial rounding-sized difference within tolerance`() {
        val linked = source(balance = 500.00, linkedDebtId = "d1")
        val debts = listOf(debt("d1", balance = 500.005)) // half a cent apart after rounding

        assertThat(detectReconciliationConflicts(listOf(linked), debts)).isEmpty()
    }

    @Test
    fun `detectReconciliationConflicts flags a real drift between linked balances`() {
        val linked = source(balance = 500.0, linkedDebtId = "d1")
        val debts = listOf(debt("d1", balance = 800.0))

        val conflicts = detectReconciliationConflicts(listOf(linked), debts)

        assertThat(conflicts).hasSize(1)
        val conflict = conflicts.single()
        assertThat(conflict.sourceBalance).isEqualTo(Money.fromMajor(500.0))
        assertThat(conflict.debtBalance).isEqualTo(Money.fromMajor(800.0))
        assertThat(conflict.difference).isEqualTo(Money.fromMajor(-300.0))
    }

    @Test
    fun `detectReconciliationConflicts skips a dangling link to a debt that no longer exists rather than crashing`() {
        val danglingLink = source(balance = 500.0, linkedDebtId = "does-not-exist")

        assertThat(detectReconciliationConflicts(listOf(danglingLink), emptyList())).isEmpty()
    }
}
