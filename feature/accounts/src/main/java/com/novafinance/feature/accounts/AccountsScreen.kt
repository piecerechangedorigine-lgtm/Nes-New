package com.novafinance.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaConfirmDialog
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaHeroBalanceCard
import com.novafinance.core.designsystem.component.NovaSectionHeader
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.CreditUtilizationLabel
import com.novafinance.core.domain.model.Debt
import com.novafinance.core.domain.model.DebtDirection
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.effectiveCreditCardUtilization
import com.novafinance.core.domain.model.formatted

@Composable
fun AccountsRoute(
    onOpenAccountTransactions: (accountId: String) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormSheetVisible by viewModel.isFormSheetVisible.collectAsStateWithLifecycle()
    val editingSource by viewModel.editingSource.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    AccountsScreen(
        uiState = uiState,
        onAccountClick = onOpenAccountTransactions,
        onAddAccountClick = viewModel::onAddAccountClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (isFormSheetVisible) {
        AccountFormSheet(
            existing = editingSource,
            linkableDebts = uiState.debts.filter { it.direction == DebtDirection.I_OWE && it.isActive && !it.isPaidOff },
            onDismiss = viewModel::onDismissFormSheet,
            onSubmit = viewModel::submitForm
        )
    }

    if (pendingDelete != null) {
        NovaConfirmDialog(
            title = "Delete ${pendingDelete?.name}?",
            message = "This also deletes every transaction recorded against this account. This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDeleteConfirmation
        )
    }
}

@Composable
private fun AccountsScreen(
    uiState: AccountsUiState,
    onAccountClick: (String) -> Unit,
    onAddAccountClick: () -> Unit,
    onEditClick: (FinancialSource) -> Unit,
    onDeleteClick: (FinancialSource) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAccountClick,
                containerColor = Nova.colors.primary,
                contentColor = Nova.colors.onPrimary
            ) {
                Icon(imageVector = NovaIcons.Plus, contentDescription = "Add account")
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Nova.colors.background),
            contentPadding = PaddingValues(
                start = Nova.spacing.screenHorizontal,
                end = Nova.spacing.screenHorizontal,
                top = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical,
                bottom = scaffoldPadding.calculateBottomPadding() + Nova.spacing.xxxl
            ),
            verticalArrangement = Arrangement.spacedBy(Nova.spacing.sectionGap)
        ) {
            item {
                NovaHeroBalanceCard(
                    label = "Total across accounts",
                    amountText = uiState.totalBalance.formatted(),
                    deltaText = null,
                    isDeltaPositive = true
                )
            }

            item {
                NovaSectionHeader(title = "Your accounts")
            }

            if (uiState.sources.isEmpty() && !uiState.isLoading) {
                item {
                    NovaEmptyState(
                        icon = NovaIcons.Wallet,
                        title = "No accounts yet",
                        message = "Add a checking, savings, or card account to start tracking your balance."
                    )
                }
            } else {
                items(uiState.sources, key = { it.id }) { source ->
                    AccountRow(
                        source = source,
                        debts = uiState.debts,
                        onClick = { onAccountClick(source.id) },
                        onEditClick = { onEditClick(source) },
                        onDeleteClick = { onDeleteClick(source) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountRow(
    source: FinancialSource,
    debts: List<Debt>,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    NovaCard(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = source.name, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
                Text(text = source.type.displayName, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
                effectiveCreditCardUtilization(source, debts)?.let { utilization ->
                    Text(
                        text = "${utilization.utilizationPercent}% used · ${utilizationLabelText(utilization.label)}",
                        style = Nova.typography.labelSmall,
                        color = utilizationTint(utilization.label)
                    )
                }
                if (!source.isActive) {
                    Text(text = "Inactive", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = source.currentBalance.formatted(),
                    style = Nova.typography.numericMedium,
                    color = if (source.type.isLiability) Nova.colors.error else Nova.colors.textPrimary
                )
                IconButton(onClick = onEditClick, modifier = Modifier.padding(start = Nova.spacing.xs)) {
                    Icon(
                        imageVector = NovaIcons.Edit,
                        contentDescription = "Edit ${source.name}",
                        tint = Nova.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = NovaIcons.Close,
                        contentDescription = "Delete ${source.name}",
                        tint = Nova.colors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun utilizationLabelText(label: CreditUtilizationLabel): String = when (label) {
    CreditUtilizationLabel.HEALTHY -> "Healthy"
    CreditUtilizationLabel.MODERATE -> "Moderate"
    CreditUtilizationLabel.HIGH_UTILIZATION -> "High utilization"
    CreditUtilizationLabel.CRITICAL -> "Critical"
}

@Composable
private fun utilizationTint(label: CreditUtilizationLabel) = when (label) {
    CreditUtilizationLabel.HEALTHY -> Nova.colors.success
    CreditUtilizationLabel.MODERATE -> Nova.colors.primary
    CreditUtilizationLabel.HIGH_UTILIZATION -> Nova.colors.warning
    CreditUtilizationLabel.CRITICAL -> Nova.colors.error
}
