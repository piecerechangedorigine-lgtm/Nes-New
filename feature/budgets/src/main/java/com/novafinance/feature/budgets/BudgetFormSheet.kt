package com.novafinance.feature.budgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.TransactionCategory

/**
 * Handles both "set a new budget" and "edit an existing one". Category
 * is picked freely for a new budget (picking one that already has a
 * budget this month replaces it — see BudgetsViewModel.submitForm) but
 * locked once [existing] is non-null: category+month is a budget's
 * deterministic identity, so letting someone change it mid-edit would
 * silently create a different budget row instead of editing this one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetFormSheet(
    existing: Budget? = null,
    onDismiss: () -> Unit,
    onSubmit: (category: TransactionCategory, monthlyLimit: Money) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val budgetableCategories = remember { TransactionCategory.entries.filterNot { it.isIncomeCategory } }
    var category by remember { mutableStateOf(existing?.category ?: budgetableCategories.first()) }
    var limitText by remember { mutableStateOf(existing?.monthlyLimit?.toMajorDouble()?.toString().orEmpty()) }

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
                text = if (existing == null) "Set a budget" else "Edit budget",
                style = Nova.typography.headlineMedium,
                color = Nova.colors.textPrimary
            )

            if (existing == null) {
                Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                    Text(text = "Category", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                    NovaChipRow(
                        options = budgetableCategories.map { NovaChipOption(it, it.displayName) },
                        selected = category,
                        onSelect = { category = it }
                    )
                }
            } else {
                Text(text = existing.category.displayName, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
            }

            NovaTextField(
                value = limitText,
                onValueChange = { input -> limitText = input.filter { it.isDigit() || it == '.' } },
                label = "Monthly limit",
                isNumeric = true,
                modifier = Modifier.fillMaxWidth()
            )

            NovaPrimaryButton(
                text = if (existing == null) "Save budget" else "Save changes",
                enabled = limitText.toDoubleOrNull() != null,
                onClick = {
                    val amount = limitText.toDoubleOrNull() ?: 0.0
                    onSubmit(category, Money.fromMajor(amount))
                }
            )
        }
    }
}
