package com.novafinance.feature.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaConfirmDialog
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.formatted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtRoute(
    onBack: () -> Unit,
    onOpenSimulator: () -> Unit,
    viewModel: DebtViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormSheetVisible by viewModel.isFormSheetVisible.collectAsStateWithLifecycle()
    val editingDebt by viewModel.editingDebt.collectAsStateWithLifecycle()
    val pendingDirection by viewModel.pendingDirection.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    DebtScreen(
        uiState = uiState,
        onBack = onBack,
        onOpenSimulator = onOpenSimulator,
        onAddOwed = { viewModel.onAddDebtClick(DebtDirection.I_OWE) },
        onAddReceivable = { viewModel.onAddDebtClick(DebtDirection.OWED_TO_ME) },
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (isFormSheetVisible) {
        DebtFormSheet(
            direction = editingDebt?.direction ?: pendingDirection,
            existing = editingDebt,
            onDismiss = viewModel::onDismissFormSheet,
            onSubmit = viewModel::submitForm
        )
    }

    if (pendingDelete != null) {
        NovaConfirmDialog(
            title = "Delete \"${pendingDelete?.name}\"?",
            message = "This removes it from your Debt Health, Debt Freedom projection, and every Debt widget. This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDeleteConfirmation
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtScreen(
    uiState: DebtUiState,
    onBack: () -> Unit,
    onOpenSimulator: () -> Unit,
    onAddOwed: () -> Unit,
    onAddReceivable: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Debt Intelligence Center") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = NovaIcons.Close, contentDescription = "Back", tint = Nova.colors.textPrimary)
                    }
                },
                actions = {
                    TextButton(onClick = onOpenSimulator) {
                        Text("Simulate", color = Nova.colors.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Nova.colors.background,
                    titleContentColor = Nova.colors.textPrimary
                )
            )
        }
    ) { scaffoldPadding ->
        when {
            uiState.errorMessage != null -> {
                NovaEmptyState(
                    icon = NovaIcons.Close,
                    title = "Couldn't load your debt data",
                    message = uiState.errorMessage,
                    modifier = Modifier.fillMaxSize().background(Nova.colors.background).padding(scaffoldPadding)
                )
            }

            uiState.summary != null -> {
                DebtContent(
                    summary = uiState.summary,
                    contentPadding = PaddingValues(
                        horizontal = Nova.spacing.screenHorizontal,
                        vertical = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical
                    ),
                    onAddOwed = onAddOwed,
                    onAddReceivable = onAddReceivable,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
private fun DebtContent(
    summary: DebtSummary,
    contentPadding: PaddingValues,
    onAddOwed: () -> Unit,
    onAddReceivable: () -> Unit,
    onEditClick: (Debt) -> Unit,
    onDeleteClick: (Debt) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Nova.colors.background),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.md)
    ) {
        item { DebtHealthHeader(summary) }

        item {
            SectionHeaderRow(title = "Money you owe", onAddClick = onAddOwed)
        }
        if (summary.activeOwedDebts.isEmpty()) {
            item {
                Text(text = "Nothing tracked here.", style = Nova.typography.bodySmall, color = Nova.colors.textTertiary)
            }
        } else {
            items(summary.activeOwedDebts, key = { it.id }) { debt ->
                DebtRow(debt = debt, onClick = { onEditClick(debt) }, onLongClick = { onDeleteClick(debt) })
            }
        }

        item {
            SectionHeaderRow(title = "Money owed to you", onAddClick = onAddReceivable)
        }
        if (summary.activeReceivables.isEmpty()) {
            item {
                Text(text = "Nothing tracked here.", style = Nova.typography.bodySmall, color = Nova.colors.textTertiary)
            }
        } else {
            items(summary.activeReceivables, key = { it.id }) { debt ->
                DebtRow(debt = debt, onClick = { onEditClick(debt) }, onLongClick = { onDeleteClick(debt) })
            }
        }

        item {
            Text(
                text = "Tap to edit · hold to delete",
                style = Nova.typography.labelSmall,
                color = Nova.colors.textTertiary
            )
        }
    }
}

@Composable
private fun DebtHealthHeader(summary: DebtSummary) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Debt health", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
        Text(
            text = "${summary.health.score}/100 · ${summary.health.label.name.lowercase().replace('_', ' ')}",
            style = Nova.typography.headlineMedium,
            color = Nova.colors.textPrimary
        )
        Text(text = summary.health.explanation, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)

        val freedomText = summary.freedomProjection.debtFreeDate?.let {
            "Projected debt-free: $it"
        } ?: "Debt-free date can't be projected yet — add a monthly payment to each debt."
        Text(text = freedomText, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
    }
}

@Composable
private fun SectionHeaderRow(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
        TextButton(onClick = onAddClick) {
            Text("Add", color = Nova.colors.primary)
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DebtRow(debt: Debt, onClick: () -> Unit, onLongClick: () -> Unit) {
    NovaCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = debt.name, style = Nova.typography.bodyLarge, color = Nova.colors.textPrimary)
                Text(text = debt.type.displayName, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = debt.currentBalance.formatted(), style = Nova.typography.numericMedium, color = Nova.colors.textPrimary)
                if (debt.dueDate != null) {
                    Text(text = debt.dueDate.toString(), style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
                }
            }
        }
    }
}
