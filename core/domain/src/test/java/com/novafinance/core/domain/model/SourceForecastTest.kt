package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import java.time.LocalDate
import org.junit.Test

class SourceForecastTest {

    private val today = LocalDate.of(2026, 1, 10) // day 10 of a 31-day month

    private val source = FinancialSource(
        id = "src-1",
        name = "Wallet",
        type = FinancialSourceType.CASH,
        currentBalance = Money.fromMajor(1000.0),
        availableBalance = Money.fromMajor(1000.0),
        createdAt = Instant.now()
    )

    private fun transaction(amount: Double) = Transaction(
        id = "txn-$amount",
        accountId = source.id,
        merchant = "Merchant",
        category = TransactionCategory.OTHER,
        amount = Money.fromMajor(amount),
        date = today
    )

    @Test
    fun `no transactions projects a flat balance and a stable trend`() {
        val forecast = calculateSourceForecast(source, emptyList(), today)

        assertThat(forecast.projectedEndOfMonthBalance).isEqualTo(source.currentBalance)
        assertThat(forecast.trend).isEqualTo(DebtTrend.STABLE)
    }

    @Test
    fun `consistent net spending projects a lower end of month balance and a worsening trend`() {
        val transactions = listOf(transaction(-100.0)) // one expense over 10 days so far

        val forecast = calculateSourceForecast(source, transactions, today)

        assertThat(forecast.projectedEndOfMonthBalance.minorUnits).isLessThan(source.currentBalance.minorUnits)
        assertThat(forecast.trend).isEqualTo(DebtTrend.WORSENING)
    }

    @Test
    fun `consistent net income projects a higher end of month balance and an improving trend`() {
        val transactions = listOf(transaction(200.0))

        val forecast = calculateSourceForecast(source, transactions, today)

        assertThat(forecast.projectedEndOfMonthBalance.minorUnits).isGreaterThan(source.currentBalance.minorUnits)
        assertThat(forecast.trend).isEqualTo(DebtTrend.IMPROVING)
    }

    @Test
    fun `a projection that goes negative is flagged as high risk`() {
        val transactions = listOf(transaction(-900.0))

        val forecast = calculateSourceForecast(source, transactions, today)

        assertThat(forecast.risk).isEqualTo(SourceForecastRisk.HIGH)
    }

    @Test
    fun `the message names the source and includes the projected figure`() {
        val forecast = calculateSourceForecast(source, emptyList(), today)

        assertThat(forecast.message).contains("Wallet")
        assertThat(forecast.message).contains(forecast.projectedEndOfMonthBalance.formatted())
    }
}
