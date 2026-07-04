package com.novafinance.feature.accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaPrimaryButton
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.FinancialSourceType
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.formattedWithSign

/** Every field this form can submit, bundled so [AccountFormSheet.onSubmit] doesn't need an 11-parameter lambda. */
data class AccountFormResult(
    val name: String,
    val type: FinancialSourceType,
    val balance: Money,
    val notes: String?,
    val creditLimit: Money?,
    val includeInLiquidity: Boolean,
    val includeInSpendingPower: Boolean,
    val includeInForecast: Boolean,
    val includeInGoals: Boolean,
    val includeInAnalytics: Boolean,
    val isEmergencyReserve: Boolean,
    val linkedDebtId: String?
)

/**
 * Handles both "add" and "edit" — [existing] being non-null is the only
 * difference, so the form itself (fields, validation, layout) can never
 * quietly drift between the two flows the way two separate composables
 * eventually would.
 *
 * Onboarding stays simple per 11.13's own instruction: the credit limit
 * field only appears once [FinancialSourceType.CREDIT_CARD] is picked
 * (progressive disclosure, not a field nobody else needs), and every
 * inclusion control (11.4) lives behind a collapsed "Advanced" section
 * that defaults closed — someone adding their first checking account
 * never has to think about Spending Power inclusion to finish the form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountFormSheet(
    existing: FinancialSource? = null,
    linkableDebts: List<Debt> = emptyList(),
    onDismiss: () -> Unit,
    onSubmit: (AccountFormResult) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember { mutableStateOf(existing?.type ?: FinancialSourceType.BANK_ACCOUNT) }
    var balanceText by remember { mutableStateOf(existing?.currentBalance?.let { it.toMajorDouble().toString() }.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var creditLimitText by remember { mutableStateOf(existing?.creditLimit?.toMajorDouble()?.toString().orEmpty()) }
    var linkedDebtId by remember { mutableStateOf(existing?.linkedDebtId) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var includeInLiquidity by remember { mutableStateOf(existing?.includeInLiquidity ?: true) }
    var includeInSpendingPower by remember { mutableStateOf(existing?.includeInSpendingPower ?: true) }
    var includeInForecast by remember { mutableStateOf(existing?.includeInForecast ?: true) }
    var includeInGoals by remember { mutableStateOf(existing?.includeInGoals ?: true) }
    var includeInAnalytics by remember { mutableStateOf(existing?.includeInAnalytics ?: true) }
    var isEmergencyReserve by remember { mutableStateOf(existing?.isEmergencyReserve ?: false) }

    val parsedBalance = balanceText.toDoubleOrNull()
    val isBalanceValid = parsedBalance != null
    val showBalanceError = balanceText.isNotBlank() && !isBalanceValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Nova.colors.elevatedSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Nova.spacing.screenHorizontal)
                .padding(bottom = Nova.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Nova.spacing.lg)
        ) {
            Text(
                text = if (existing == null) "Add account" else "Edit account",
                style = Nova.typography.headlineMedium,
                color = Nova.colors.textPrimary
            )

            NovaTextField(
                value = name,
                onValueChange = { name = it },
                label = "Account name",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                Text(text = "Type", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = FinancialSourceType.entries.map { NovaChipOption(it, it.displayName) },
                    selected = type,
                    onSelect = { type = it }
                )
            }

            NovaTextField(
                value = balanceText,
                onValueChange = { input -> balanceText = input.filter { it.isDigit() || it == '.' || it == '-' } },
                label = "Current balance",
                isNumeric = true,
                isError = showBalanceError,
                supportingText = if (showBalanceError) "Enter a valid amount" else null,
                modifier = Modifier.fillMaxWidth()
            )

            // Progressive disclosure (11.5/11.13) — a credit limit means
            // nothing for any other source type, so it only exists on
            // screen once there's a reason for it.
            if (type == FinancialSourceType.CREDIT_CARD) {
                NovaTextField(
                    value = creditLimitText,
                    onValueChange = { input -> creditLimitText = input.filter { it.isDigit() || it == '.' } },
                    label = "Credit limit (optional)",
                    isNumeric = true,
                    supportingText = "Enables utilization tracking",
                    modifier = Modifier.fillMaxWidth()
                )

                if (linkableDebts.isNotEmpty()) {
                    LinkedDebtPicker(
                        debts = linkableDebts,
                        selectedDebtId = linkedDebtId,
                        onSelect = { linkedDebtId = it }
                    )
                }
            }

            NovaTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes (optional)",
                modifier = Modifier.fillMaxWidth()
            )

            if (existing != null && parsedBalance != null) {
                val delta = Money.fromMajor(parsedBalance) - existing.currentBalance
                if (delta.minorUnits != 0L) {
                    Text(
                        text = "Balance will change by ${delta.formattedWithSign()}",
                        style = Nova.typography.bodySmall,
                        color = Nova.colors.textSecondary
                    )
                }
            }

            AdvancedInclusionSection(
                isExpanded = isAdvancedExpanded,
                onToggleExpanded = { isAdvancedExpanded = !isAdvancedExpanded },
                includeInLiquidity = includeInLiquidity,
                onIncludeInLiquidityChange = { includeInLiquidity = it },
                includeInSpendingPower = includeInSpendingPower,
                onIncludeInSpendingPowerChange = { includeInSpendingPower = it },
                includeInForecast = includeInForecast,
                onIncludeInForecastChange = { includeInForecast = it },
                includeInGoals = includeInGoals,
                onIncludeInGoalsChange = { includeInGoals = it },
                includeInAnalytics = includeInAnalytics,
                onIncludeInAnalyticsChange = { includeInAnalytics = it },
                isEmergencyReserve = isEmergencyReserve,
                onIsEmergencyReserveChange = { isEmergencyReserve = it }
            )

            NovaPrimaryButton(
                text = if (existing == null) "Add account" else "Save changes",
                enabled = name.isNotBlank() && isBalanceValid,
                onClick = {
                    onSubmit(
                        AccountFormResult(
                            name = name,
                            type = type,
                            balance = Money.fromMajor(parsedBalance ?: 0.0),
                            notes = notes.ifBlank { null },
                            creditLimit = creditLimitText.toDoubleOrNull()?.let { Money.fromMajor(it) },
                            includeInLiquidity = includeInLiquidity,
                            includeInSpendingPower = includeInSpendingPower,
                            includeInForecast = includeInForecast,
                            includeInGoals = includeInGoals,
                            includeInAnalytics = includeInAnalytics,
                            isEmergencyReserve = isEmergencyReserve,
                            linkedDebtId = if (type == FinancialSourceType.CREDIT_CARD) linkedDebtId else null
                        )
                    )
                }
            )
        }
    }
}

/** Lets a credit card claim ownership handoff to a [Debt] record per Phase 11.5.1's reconciliation ownership rule — once linked, the Debt's balance becomes authoritative for this card everywhere Balance Intelligence reads it. */
@Composable
private fun LinkedDebtPicker(debts: List<Debt>, selectedDebtId: String?, onSelect: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
        Text(text = "Link to a tracked debt (optional)", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
        Text(
            text = "Once linked, the debt's balance and payoff plan become the source of truth for this card's utilization and liquidity impact.",
            style = Nova.typography.bodySmall,
            color = Nova.colors.textTertiary
        )
        NovaChipRow(
            options = listOf(NovaChipOption<String?>(null, "None")) + debts.map { NovaChipOption<String?>(it.id, it.name) },
            selected = selectedDebtId,
            onSelect = onSelect
        )
    }
}

/** The 11.4 inclusion controls plus the emergency-reserve flag, collapsed by default — see this file's own top doc comment for why. */
@Composable
private fun AdvancedInclusionSection(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    includeInLiquidity: Boolean,
    onIncludeInLiquidityChange: (Boolean) -> Unit,
    includeInSpendingPower: Boolean,
    onIncludeInSpendingPowerChange: (Boolean) -> Unit,
    includeInForecast: Boolean,
    onIncludeInForecastChange: (Boolean) -> Unit,
    includeInGoals: Boolean,
    onIncludeInGoalsChange: (Boolean) -> Unit,
    includeInAnalytics: Boolean,
    onIncludeInAnalyticsChange: (Boolean) -> Unit,
    isEmergencyReserve: Boolean,
    onIsEmergencyReserveChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm)) {
        TextButton(onClick = onToggleExpanded) {
            Text(if (isExpanded) "Hide advanced options" else "Advanced options", color = Nova.colors.primary)
        }

        if (isExpanded) {
            Text(
                text = "Choose what this account counts toward. Everything is included by default.",
                style = Nova.typography.bodySmall,
                color = Nova.colors.textSecondary
            )
            InclusionToggleRow("Total Liquidity", includeInLiquidity, onIncludeInLiquidityChange)
            InclusionToggleRow("Spending Power", includeInSpendingPower, onIncludeInSpendingPowerChange)
            InclusionToggleRow("Forecast", includeInForecast, onIncludeInForecastChange)
            InclusionToggleRow("Goal calculations", includeInGoals, onIncludeInGoalsChange)
            InclusionToggleRow("Analytics", includeInAnalytics, onIncludeInAnalyticsChange)
            InclusionToggleRow("Part of my emergency reserve", isEmergencyReserve, onIsEmergencyReserveChange)
        }
    }
}

@Composable
private fun InclusionToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = Nova.typography.bodyMedium, color = Nova.colors.textPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Nova.colors.onPrimary,
                checkedTrackColor = Nova.colors.primary,
                uncheckedThumbColor = Nova.colors.textSecondary,
                uncheckedTrackColor = Nova.colors.surface
            )
        )
    }
}
