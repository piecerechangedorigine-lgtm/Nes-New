package com.novafinance.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.color
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaConfirmDialog
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.component.NovaTransactionRow
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.TransactionCategory
import com.novafinance.core.domain.model.formattedWithSign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsRoute(
    onBack: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormSheetVisible by viewModel.isFormSheetVisible.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    TransactionsScreen(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelect = viewModel::onCategorySelect,
        onAddTransactionClick = viewModel::onAddTransactionClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (isFormSheetVisible) {
        TransactionFormSheet(
            sources = uiState.sources,
            existing = editingTransaction,
            onDismiss = viewModel::onDismissFormSheet,
            onSubmit = viewModel::submitForm
        )
    }

    if (pendingDelete != null) {
        NovaConfirmDialog(
            title = "Delete this transaction?",
            message = "\"${pendingDelete?.merchant}\" (${pendingDelete?.amount?.formattedWithSign()}) will be removed. This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDeleteConfirmation
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionsScreen(
    uiState: TransactionsUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (TransactionCategory?) -> Unit,
    onAddTransactionClick: () -> Unit,
    onEditClick: (Transaction) -> Unit,
    onDeleteClick: (Transaction) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isAccountScoped) "Account activity" else "Transactions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NovaIcons.Close, contentDescription = "Back", tint = Nova.colors.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Nova.colors.background,
                    titleContentColor = Nova.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = Nova.colors.primary,
                contentColor = Nova.colors.onPrimary
            ) {
                Icon(imageVector = NovaIcons.Plus, contentDescription = "Add transaction")
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Nova.colors.background)
                .padding(scaffoldPadding)
                .padding(horizontal = Nova.spacing.screenHorizontal)
        ) {
            NovaTextField(
                value = uiState.filters.query,
                onValueChange = onQueryChange,
                label = "Search transactions",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Nova.spacing.sm)
            )

            NovaChipRow(
                options = listOf(NovaChipOption<TransactionCategory?>(null, "All")) +
                    TransactionCategory.entries.map { NovaChipOption<TransactionCategory?>(it, it.displayName) },
                selected = uiState.filters.category,
                onSelect = onCategorySelect,
                modifier = Modifier.padding(vertical = Nova.spacing.md)
            )

            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                NovaEmptyState(
                    icon = NovaIcons.Search,
                    title = "No matching transactions",
                    message = "Try a different search or category filter."
                )
            } else {
                Text(
                    text = "Tap to edit · hold to delete",
                    style = Nova.typography.labelSmall,
                    color = Nova.colors.textTertiary,
                    modifier = Modifier.padding(bottom = Nova.spacing.xs)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Nova.spacing.xxxl),
                    verticalArrangement = Arrangement.spacedBy(Nova.spacing.xxs)
                ) {
                    items(uiState.transactions, key = { it.id }) { transaction ->
                        NovaTransactionRow(
                            merchant = transaction.merchant,
                            categoryLabel = transaction.category.displayName,
                            categoryColor = transaction.category.color,
                            dateText = transaction.date.toString(),
                            amountText = transaction.amount.formattedWithSign(),
                            isIncome = transaction.amount.isPositive,
                            onClick = { onEditClick(transaction) },
                            onLongClick = { onDeleteClick(transaction) }
                        )
                    }
                }
            }
        }
    }
}
