package com.novafinance.feature.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaPrimaryButton
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtType
import com.novafinance.core.domain.model.Money
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Handles both "add" and "edit" for either [DebtDirection] — the
 * direction itself is chosen before this sheet opens (which "Add"
 * button was tapped), not inside the form, since a debt's direction
 * never changes after creation (money you owe doesn't become money
 * owed to you). Every field except name and amounts is optional, per
 * 10.2's "Dates are optional. User decides."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormSheet(
    direction: DebtDirection,
    existing: Debt? = null,
    onDismiss: () -> Unit,
    onSubmit: (
        name: String,
        type: DebtType,
        originalAmount: Money,
        currentBalance: Money,
        interestRatePercent: Double?,
        minimumMonthlyPayment: Money?,
        dueDate: LocalDate?,
        counterpartyName: String?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var type by remember { mutableStateOf(existing?.type ?: DebtType.PERSONAL_LOAN) }
    var originalAmountText by remember { mutableStateOf(existing?.originalAmount?.toMajorDouble()?.toString().orEmpty()) }
    var currentBalanceText by remember { mutableStateOf(existing?.currentBalance?.toMajorDouble()?.toString().orEmpty()) }
    var interestText by remember { mutableStateOf(existing?.interestRatePercent?.toString().orEmpty()) }
    var paymentText by remember { mutableStateOf(existing?.minimumMonthlyPayment?.toMajorDouble()?.toString().orEmpty()) }
    var counterpartyName by remember { mutableStateOf(existing?.counterpartyName.orEmpty()) }
    var dueDate by remember { mutableStateOf(existing?.dueDate) }
    var isDatePickerVisible by remember { mutableStateOf(false) }

    val originalAmount = originalAmountText.toDoubleOrNull()
    val currentBalance = currentBalanceText.toDoubleOrNull()
    val isValid = name.isNotBlank() && originalAmount != null && currentBalance != null

    val dueDateLabel = if (direction == DebtDirection.I_OWE) "Target repayment date" else "Expected recovery date"
    val counterpartyLabel = if (direction == DebtDirection.I_OWE) "Who you owe" else "Who owes you"

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
                text = if (existing == null) {
                    if (direction == DebtDirection.I_OWE) "Add money you owe" else "Add money owed to you"
                } else "Edit debt",
                style = Nova.typography.headlineMedium,
                color = Nova.colors.textPrimary
            )

            NovaTextField(value = name, onValueChange = { name = it }, label = "Name", modifier = Modifier.fillMaxWidth())

            Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                Text(text = "Type", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = DebtType.entries.map { NovaChipOption(it, it.displayName) },
                    selected = type,
                    onSelect = { type = it }
                )
            }

            NovaTextField(
                value = originalAmountText,
                onValueChange = { input -> originalAmountText = input.filter { it.isDigit() || it == '.' } },
                label = "Original amount",
                isNumeric = true,
                modifier = Modifier.fillMaxWidth()
            )

            NovaTextField(
                value = currentBalanceText,
                onValueChange = { input -> currentBalanceText = input.filter { it.isDigit() || it == '.' } },
                label = "Current balance remaining",
                isNumeric = true,
                modifier = Modifier.fillMaxWidth()
            )

            NovaTextField(
                value = interestText,
                onValueChange = { input -> interestText = input.filter { it.isDigit() || it == '.' } },
                label = "Interest rate % (optional)",
                isNumeric = true,
                modifier = Modifier.fillMaxWidth()
            )

            NovaTextField(
                value = paymentText,
                onValueChange = { input -> paymentText = input.filter { it.isDigit() || it == '.' } },
                label = "Monthly payment (optional)",
                isNumeric = true,
                supportingText = "Needed to project a payoff date and payoff plans",
                modifier = Modifier.fillMaxWidth()
            )

            NovaTextField(
                value = counterpartyName,
                onValueChange = { counterpartyName = it },
                label = "$counterpartyLabel (optional)",
                modifier = Modifier.fillMaxWidth()
            )

            DueDateRow(label = dueDateLabel, date = dueDate, onChangeClick = { isDatePickerVisible = true }, onClearClick = { dueDate = null })

            NovaPrimaryButton(
                text = if (existing == null) "Add" else "Save changes",
                enabled = isValid,
                onClick = {
                    onSubmit(
                        name,
                        type,
                        Money.fromMajor(originalAmount ?: 0.0),
                        Money.fromMajor(currentBalance ?: 0.0),
                        interestText.toDoubleOrNull(),
                        paymentText.toDoubleOrNull()?.let { Money.fromMajor(it) },
                        dueDate,
                        counterpartyName
                    )
                }
            )
        }
    }

    if (isDatePickerVisible) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { isDatePickerVisible = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    isDatePickerVisible = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { isDatePickerVisible = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DueDateRow(label: String, date: LocalDate?, onChangeClick: () -> Unit, onClearClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = label, style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
            Text(
                text = date?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "None set",
                style = Nova.typography.bodyMedium,
                color = Nova.colors.textPrimary
            )
        }
        Row {
            if (date != null) {
                TextButton(onClick = onClearClick) { Text("Clear", color = Nova.colors.textSecondary) }
            }
            TextButton(onClick = onChangeClick) { Text(if (date == null) "Set date" else "Change", color = Nova.colors.primary) }
        }
    }
}
