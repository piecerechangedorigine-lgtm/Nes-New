package com.novafinance.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import com.novafinance.core.designsystem.component.NovaConfirmDialog
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaProgressCard
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.Budget
import com.novafinance.core.domain.model.BudgetProgress
import com.novafinance.core.domain.model.formatted

@Composable
fun BudgetsRoute(
    onBack: () -> Unit,
    viewModel: BudgetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormSheetVisible by viewModel.isFormSheetVisible.collectAsStateWithLifecycle()
    val editingBudget by viewModel.editingBudget.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    BudgetsScreen(
        uiState = uiState,
        onBack = onBack,
        onAddBudgetClick = viewModel::onAddBudgetClick,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (isFormSheetVisible) {
        BudgetFormSheet(
            existing = editingBudget,
            onDismiss = viewModel::onDismissFormSheet,
            onSubmit = viewModel::submitForm
        )
    }

    if (pendingDelete != null) {
        NovaConfirmDialog(
            title = "Delete ${pendingDelete?.category?.displayName} budget?",
            message = "This month's spending limit for this category will be removed. Your transactions aren't affected.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDeleteConfirmation
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetsScreen(
    uiState: BudgetsUiState,
    onBack: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onEditClick: (Budget) -> Unit,
    onDeleteClick: (Budget) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Budgets") },
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
                onClick = onAddBudgetClick,
                containerColor = Nova.colors.primary,
                contentColor = Nova.colors.onPrimary
            ) {
                Icon(imageVector = NovaIcons.Plus, contentDescription = "Set budget")
            }
        }
    ) { scaffoldPadding ->
        if (uiState.errorMessage != null) {
            NovaEmptyState(
                icon = NovaIcons.Close,
                title = "Couldn't load your budgets",
                message = uiState.errorMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Nova.colors.background)
                    .padding(scaffoldPadding)
            )
        } else if (uiState.progress.isEmpty() && !uiState.isLoading) {
            NovaEmptyState(
                icon = NovaIcons.Target,
                title = "No budgets set",
                message = "Set a monthly limit per category to start tracking spend against it.",
                modifier = Modifier
                    .fillMaxSize()
                    .background(Nova.colors.background)
                    .padding(scaffoldPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Nova.colors.background),
                contentPadding = PaddingValues(
                    horizontal = Nova.spacing.screenHorizontal,
                    vertical = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical
                ),
                verticalArrangement = Arrangement.spacedBy(Nova.spacing.md)
            ) {
                item {
                    Text(
                        text = "Tap to edit · hold to delete",
                        style = Nova.typography.labelSmall,
                        color = Nova.colors.textTertiary
                    )
                }
                items(uiState.progress, key = { it.budget.id }) { progress ->
                    BudgetProgressCard(
                        progress = progress,
                        onClick = { onEditClick(progress.budget) },
                        onLongClick = { onDeleteClick(progress.budget) }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BudgetProgressCard(progress: BudgetProgress, onClick: () -> Unit, onLongClick: () -> Unit) {
    val subtitle = if (progress.isOverLimit) {
        "${progress.spent.formatted()} of ${progress.budget.monthlyLimit.formatted()} — over limit"
    } else {
        "${progress.spent.formatted()} of ${progress.budget.monthlyLimit.formatted()}"
    }
    NovaProgressCard(
        title = progress.budget.category.displayName,
        subtitle = subtitle,
        progress = progress.percentUsed,
        accentColor = progress.budget.category.color,
        isOverLimit = progress.isOverLimit,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}
