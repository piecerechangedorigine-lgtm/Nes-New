package com.novafinance.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackupPayloadTest {

    private fun samplePayload() = BackupPayload(
        schemaVersion = CURRENT_BACKUP_VERSION,
        exportedAtEpochMillis = 1_700_000_000_000,
        sources = listOf(
            BackupSourceDto(
                id = "src-1",
                name = "Checking",
                type = "BANK_ACCOUNT",
                currentBalanceMinorUnits = 250_000,
                availableBalanceMinorUnits = 250_000,
                currency = "USD",
                notes = null,
                isActive = true,
                balanceUpdateMode = "MANUAL",
                createdAtEpochMillis = 1_600_000_000_000
            )
        ),
        transactions = listOf(
            BackupTransactionDto(
                id = "txn-1",
                accountId = "src-1",
                merchant = "Grocery",
                category = "FOOD",
                amountMinorUnits = -5_000,
                date = "2026-01-15",
                note = "weekly shop"
            )
        ),
        budgets = listOf(
            BackupBudgetDto(id = "budget-1", category = "FOOD", monthlyLimitMinorUnits = 30_000, month = "2026-01")
        ),
        goals = listOf(
            BackupGoalDto(
                id = "goal-1",
                name = "Wedding fund",
                targetAmountMinorUnits = 500_000,
                currentAmountMinorUnits = 100_000,
                targetDate = "2027-06-01",
                createdAt = "2026-01-01"
            )
        ),
        debts = listOf(
            BackupDebtDto(
                id = "debt-1",
                name = "Car loan",
                direction = "I_OWE",
                type = "CAR_LOAN",
                originalAmountMinorUnits = 1_000_000,
                currentBalanceMinorUnits = 400_000,
                interestRatePercent = 6.5,
                minimumMonthlyPaymentMinorUnits = 25_000,
                dueDate = null,
                counterpartyName = "Auto Finance Co",
                notes = null,
                isActive = true,
                createdAt = "2025-06-01"
            )
        )
    )

    @Test
    fun `round trips through JSON without losing any data`() {
        val original = samplePayload()

        val restored = parseBackupPayload(original.toJson())

        assertThat(restored).isEqualTo(original)
    }

    @Test
    fun `a v1 backup with no debts field at all still parses, defaulting debts to empty`() {
        // Simulates a real Phase 8.5 backup file, written before Phase
        // 10 added debt tracking — the "debts" key never existed in it.
        val v1Json = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMillis": 1700000000000,
              "sources": [],
              "transactions": [],
              "budgets": [],
              "goals": []
            }
        """.trimIndent()

        val restored = parseBackupPayload(v1Json)

        assertThat(restored.debts).isEmpty()
        assertThat(restored.schemaVersion).isEqualTo(1)
    }

    @Test
    fun `a backup from a newer app version is rejected with a clear message`() {
        val futurePayload = samplePayload().copy(schemaVersion = CURRENT_BACKUP_VERSION + 1)

        val exception = org.junit.Assert.assertThrows(InvalidBackupException::class.java) {
            parseBackupPayload(futurePayload.toJson())
        }

        assertThat(exception.message).contains("newer version")
    }

    @Test
    fun `garbage input is rejected rather than throwing a raw serialization exception`() {
        val exception = org.junit.Assert.assertThrows(InvalidBackupException::class.java) {
            parseBackupPayload("this is not json at all")
        }

        assertThat(exception.message).contains("doesn't look like a valid Nova backup")
    }

    @Test
    fun `unknown fields in a future schema are ignored rather than failing to parse`() {
        val jsonWithExtraField = samplePayload().toJson()
            .replaceFirst("\"schemaVersion\"", "\"someFutureField\": true, \"schemaVersion\"")

        val restored = parseBackupPayload(jsonWithExtraField)

        assertThat(restored.sources).hasSize(1)
    }
}
