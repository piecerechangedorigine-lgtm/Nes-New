package com.novafinance.feature.goals

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
import com.novafinance.core.designsystem.component.NovaOutlinedCard
import com.novafinance.core.designsystem.component.NovaPrimaryButton
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.formatted
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Handles both "create a new goal" and "edit an existing one". Editing
 * only ever changes name/target amount/target date — [SavingsGoal.currentAmount]
 * is deliberately untouched by this sheet, since contributions go
 * through the separate "Add funds" flow on the goal card; folding that
 * into an edit form would make it too easy to accidentally overwrite
 * real contribution history with a typo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalFormSheet(
    existing: SavingsGoal? = null,
    onDismiss: () -> Unit,
    onSubmit: (name: String, targetAmount: Money, targetDate: LocalDate?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var amountText by remember { mutableStateOf(existing?.targetAmount?.toMajorDouble()?.toString().orEmpty()) }
    var targetDate by remember { mutableStateOf(existing?.targetDate) }
    var isDatePickerVisible by remember { mutableStateOf(false) }

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
                text = if (existing == null) "New savings goal" else "Edit goal",
                style = Nova.typography.headlineMedium,
                color = Nova.colors.textPrimary
            )

            NovaTextField(
                value = name,
                onValueChange = { name = it },
                label = "Goal name",
                modifier = Modifier.fillMaxWidth()
            )

            NovaTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = "Target amount",
                isNumeric = true,
                supportingText = if (existing != null) {
                    "Currently ${existing.currentAmount.formatted()} saved"
                } else null,
                modifier = Modifier.fillMaxWidth()
            )

            NovaOutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Target date", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                        Text(
                            text = targetDate?.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "Optional — no deadline",
                            style = Nova.typography.bodyMedium,
                            color = Nova.colors.textPrimary
                        )
                    }
                    TextButton(onClick = { isDatePickerVisible = true }) {
                        Text(if (targetDate == null) "Set date" else "Change", color = Nova.colors.primary)
                    }
                }
            }

            NovaPrimaryButton(
                text = if (existing == null) "Create goal" else "Save changes",
                enabled = name.isNotBlank() && amountText.toDoubleOrNull() != null,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onSubmit(name, Money.fromMajor(amount), targetDate)
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
                        targetDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
