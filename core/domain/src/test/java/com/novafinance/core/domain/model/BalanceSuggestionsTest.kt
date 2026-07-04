package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class BalanceSuggestionsTest {

    private fun source(balance: Double, type: FinancialSourceType = FinancialSourceType.BANK_ACCOUNT, creditLimit: Double? = null) = FinancialSource(
        id = "src-$type-$balance",
        name = type.displayName,
        type = type,
        currentBalance = Money.fromMajor(balance),
        availableBalance = Money.fromMajor(balance),
        creditLimit = creditLimit?.let { Money.fromMajor(it) },
        createdAt = Instant.now()
    )

    private fun overview(spendingPower: Double = 0.0, emergencyReserve: Double = 0.0) = BalanceOverview(
        totalLiquidity = Money.fromMajor(spendingPower),
        availableSpendingPower = Money.fromMajor(spendingPower),
        dreamSafeBalance = Money.ZERO,
        emergencyReserve = Money.fromMajor(emergencyReserve),
        forecastBalance = Money.fromMajor(spendingPower)
    )

    @Test
    fun `a critically utilized credit card produces a warning suggestion naming it`() {
        val maxedCard = source(balance = 900.0, type = FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0)

        val suggestions = generateBalanceSuggestions(listOf(maxedCard), overview(), emptyList(), Money.fromMajor(1000.0))

        val match = suggestions.single { it.message.contains(maxedCard.name) }
        assertThat(match.priority).isEqualTo(SuggestionPriority.WARNING)
    }

    @Test
    fun `a healthy credit card produces no suggestion at all`() {
        val healthyCard = source(balance = 100.0, type = FinancialSourceType.CREDIT_CARD, creditLimit = 1000.0)

        val suggestions = generateBalanceSuggestions(listOf(healthyCard), overview(), emptyList(), Money.fromMajor(1000.0))

        assertThat(suggestions).isEmpty()
    }

    @Test
    fun `thin emergency reserve with real spare spending power produces a suggestion`() {
        val suggestions = generateBalanceSuggestions(
            sources = emptyList(),
            overview = overview(spendingPower = 5000.0, emergencyReserve = 100.0),
            goalForecasts = emptyList(),
            monthlyExpense = Money.fromMajor(1000.0)
        )

        assertThat(suggestions.any { it.message.contains("emergency reserve") }).isTrue()
    }

    @Test
    fun `thin emergency reserve with no spare spending power produces no suggestion`() {
        val suggestions = generateBalanceSuggestions(
            sources = emptyList(),
            overview = overview(spendingPower = 200.0, emergencyReserve = 0.0),
            goalForecasts = emptyList(),
            monthlyExpense = Money.fromMajor(1000.0)
        )

        assertThat(suggestions.any { it.message.contains("emergency reserve") }).isFalse()
    }

    @Test
    fun `an underfunded goal close to its deadline produces a suggestion naming it`() {
        val goal = SavingsGoal(
            id = "g1",
            name = "Car fund",
            targetAmount = Money.fromMajor(5000.0),
            currentAmount = Money.fromMajor(500.0),
            targetDate = LocalDate.now().plusMonths(1),
            createdAt = LocalDate.now().minusMonths(5)
        )
        val forecast = goal.forecast(LocalDate.now())

        val suggestions = generateBalanceSuggestions(emptyList(), overview(), listOf(forecast), Money.fromMajor(1000.0))

        assertThat(suggestions.any { it.message.contains("Car fund") }).isTrue()
    }

    @Test
    fun `a reconciliation conflict between a linked source and its debt produces a warning naming both`() {
        val linkedCard = FinancialSource(
            id = "card-1",
            name = "Rewards Card",
            type = FinancialSourceType.CREDIT_CARD,
            currentBalance = Money.fromMajor(500.0),
            availableBalance = Money.fromMajor(500.0),
            linkedDebtId = "debt-1",
            createdAt = Instant.now()
        )
        val linkedDebt = Debt(
            id = "debt-1",
            name = "Rewards Card",
            direction = DebtDirection.I_OWE,
            type = DebtType.CREDIT_CARD,
            originalAmount = Money.fromMajor(1000.0),
            currentBalance = Money.fromMajor(900.0),
            createdAt = LocalDate.now()
        )

        val suggestions = generateBalanceSuggestions(listOf(linkedCard), overview(), emptyList(), Money.fromMajor(1000.0), debts = listOf(linkedDebt))

        val conflictSuggestion = suggestions.single { it.message.contains("Rewards Card") && it.message.contains("linked debt") }
        assertThat(conflictSuggestion.priority).isEqualTo(SuggestionPriority.WARNING)
    }

    @Test
    fun `nothing concerning produces an empty list, not a fabricated suggestion`() {
        val healthySource = source(balance = 5000.0)

        val suggestions = generateBalanceSuggestions(
            sources = listOf(healthySource),
            overview = overview(spendingPower = 5000.0, emergencyReserve = 6000.0),
            goalForecasts = emptyList(),
            monthlyExpense = Money.fromMajor(1000.0)
        )

        assertThat(suggestions).isEmpty()
    }
}
