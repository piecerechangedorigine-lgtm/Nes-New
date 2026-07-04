package com.novafinance.feature.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaPrimaryButton
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory

/**
 * Handles both "add" and "edit" — [existing] being non-null is the only
 * difference. The category chip row always has a selection (defaults to
 * [existing]'s category when editing, [TransactionCategory.OTHER]
 * otherwise), so a submitted transaction can never end up with no
 * category — that's what "preserve category integrity" means here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormSheet(
    sources: List<FinancialSource>,
    existing: Transaction? = null,
    onDismiss: () -> Unit,
    onSubmit: (sourceId: String, merchant: String, category: TransactionCategory, amount: Money) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Nova.colors.elevatedSurface
    ) {
        if (sources.isEmpty()) {
            NovaEmptyState(
                icon = NovaIcons.Wallet,
                title = "Add an account first",
                message = "You'll need at least one account before logging a transaction.",
                modifier = Modifier.padding(horizontal = Nova.spacing.screenHorizontal, vertical = Nova.spacing.xxl)
            )
            return@ModalBottomSheet
        }

        val initialSource = sources.find { it.id == existing?.accountId } ?: sources.first()
        var selectedSource by remember { mutableStateOf(initialSource) }
        var merchant by remember { mutableStateOf(existing?.merchant.orEmpty()) }
        var category by remember { mutableStateOf(existing?.category ?: TransactionCategory.OTHER) }
        var amountText by remember { mutableStateOf(existing?.amount?.let { kotlin.math.abs(it.toMajorDouble()).toString() }.orEmpty()) }
        var isExpense by remember { mutableStateOf(existing?.amount?.isNegative ?: true) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Nova.spacing.screenHorizontal)
                .padding(bottom = Nova.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Nova.spacing.lg)
        ) {
            Text(
                text = if (existing == null) "Add transaction" else "Edit transaction",
                style = Nova.typography.headlineMedium,
                color = Nova.colors.textPrimary
            )

            if (sources.size > 1) {
                Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                    Text(text = "Account", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                    NovaChipRow(
                        options = sources.map { NovaChipOption(it, it.name) },
                        selected = selectedSource,
                        onSelect = { selectedSource = it }
                    )
                }
            }

            NovaTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = "Merchant",
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                Text(text = "Category", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = TransactionCategory.entries.map { NovaChipOption(it, it.displayName) },
                    selected = category,
                    onSelect = {
                        category = it
                        isExpense = !it.isIncomeCategory
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)) {
                NovaChipRow(
                    options = listOf(
                        NovaChipOption(true, "Expense"),
                        NovaChipOption(false, "Income")
                    ),
                    selected = isExpense,
                    onSelect = { isExpense = it }
                )
            }

            NovaTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = "Amount",
                isNumeric = true,
                modifier = Modifier.fillMaxWidth()
            )

            NovaPrimaryButton(
                text = if (existing == null) "Add transaction" else "Save changes",
                enabled = merchant.isNotBlank() && amountText.toDoubleOrNull() != null,
                onClick = {
                    val magnitude = amountText.toDoubleOrNull() ?: 0.0
                    val signedAmount = Money.fromMajor(if (isExpense) -magnitude else magnitude)
                    onSubmit(selectedSource.id, merchant, category, signedAmount)
                }
            )
        }
    }
}
