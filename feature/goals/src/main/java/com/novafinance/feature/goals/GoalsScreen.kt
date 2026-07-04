package com.novafinance.feature.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaConfirmDialog
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaLinearProgressBar
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.GoalHealthLabel
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.SavingsGoal
import com.novafinance.core.domain.model.formatted
import com.novafinance.core.domain.model.health

@Composable
fun GoalsRoute(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFormSheetVisible by viewModel.isFormSheetVisible.collectAsStateWithLifecycle()
    val editingGoal by viewModel.editingGoal.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    GoalsScreen(
        uiState = uiState,
        onBack = onBack,
        onAddGoalClick = viewModel::onAddGoalClick,
        onContribute = viewModel::contribute,
        onEditClick = viewModel::onEditClick,
        onDeleteClick = viewModel::onDeleteClick
    )

    if (isFormSheetVisible) {
        GoalFormSheet(
            existing = editingGoal,
            onDismiss = viewModel::onDismissFormSheet,
            onSubmit = viewModel::submitForm
        )
    }

    if (pendingDelete != null) {
        NovaConfirmDialog(
            title = "Delete \"${pendingDelete?.name}\"?",
            message = "You'll lose the ${pendingDelete?.currentAmount?.formatted()} progress recorded toward this goal. This can't be undone.",
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::onDismissDeleteConfirmation
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalsScreen(
    uiState: GoalsUiState,
    onBack: () -> Unit,
    onAddGoalClick: () -> Unit,
    onContribute: (goalId: String, amount: Money) -> Unit,
    onEditClick: (SavingsGoal) -> Unit,
    onDeleteClick: (SavingsGoal) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Goals") },
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
                onClick = onAddGoalClick,
                containerColor = Nova.colors.primary,
                contentColor = Nova.colors.onPrimary
            ) {
                Icon(imageVector = NovaIcons.Plus, contentDescription = "New goal")
            }
        }
    ) { scaffoldPadding ->
        if (uiState.errorMessage != null) {
            NovaEmptyState(
                icon = NovaIcons.Close,
                title = "Couldn't load your goals",
                message = uiState.errorMessage,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Nova.colors.background)
                    .padding(scaffoldPadding)
            )
        } else if (uiState.forecasts.isEmpty() && !uiState.isLoading) {
            NovaEmptyState(
                icon = NovaIcons.Target,
                title = "No savings goals yet",
                message = "Create a goal to start tracking progress toward it.",
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
                items(uiState.forecasts, key = { it.goal.id }) { forecast ->
                    GoalCard(
                        forecast = forecast,
                        onContribute = { amount -> onContribute(forecast.goal.id, amount) },
                        onEditClick = { onEditClick(forecast.goal) },
                        onDeleteClick = { onDeleteClick(forecast.goal) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalCard(
    forecast: GoalForecast,
    onContribute: (Money) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isContributeDialogVisible by remember { mutableStateOf(false) }
    val goal = forecast.goal
    val health = remember(forecast) { forecast.health() }

    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = goal.name, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
                Text(
                    text = "${health.label.name.lowercase().replaceFirstChar(Char::uppercase)} · ${health.score}/100",
                    style = Nova.typography.labelSmall,
                    color = healthColor(health.label)
                )
            }
            Row {
                TextButton(onClick = { isContributeDialogVisible = true }) {
                    Text("Add funds", color = Nova.colors.primary)
                }
                TextButton(onClick = onEditClick) {
                    Text("Edit", color = Nova.colors.textSecondary)
                }
                TextButton(onClick = onDeleteClick) {
                    Text("Delete", color = Nova.colors.error)
                }
            }
        }
        Text(
            text = "${goal.currentAmount.formatted()} of ${goal.targetAmount.formatted()}",
            style = Nova.typography.bodyMedium,
            color = Nova.colors.textSecondary
        )
        NovaLinearProgressBar(progress = goal.percentComplete, color = Nova.colors.primary)
        if (forecast.requiredMonthlyContribution != null) {
            Text(
                text = "${forecast.requiredMonthlyContribution?.formatted().orEmpty()}/month to hit your target date",
                style = Nova.typography.bodySmall,
                color = Nova.colors.textSecondary
            )
        } else if (goal.isComplete) {
            Text(text = "Goal reached", style = Nova.typography.bodySmall, color = Nova.colors.success)
        }
    }

    if (isContributeDialogVisible) {
        ContributeDialog(
            onDismiss = { isContributeDialogVisible = false },
            onConfirm = { amount ->
                onContribute(amount)
                isContributeDialogVisible = false
            }
        )
    }
}

@Composable
private fun healthColor(label: GoalHealthLabel) = when (label) {
    GoalHealthLabel.EXCELLENT -> Nova.colors.success
    GoalHealthLabel.GOOD -> Nova.colors.primary
    GoalHealthLabel.AT_RISK -> Nova.colors.warning
    GoalHealthLabel.CRITICAL -> Nova.colors.error
}

@Composable
private fun ContributeDialog(onDismiss: () -> Unit, onConfirm: (Money) -> Unit) {
    var amountText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nova.colors.elevatedSurface,
        title = { Text("Add funds", color = Nova.colors.textPrimary) },
        text = {
            NovaTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() || it == '.' } },
                label = "Amount",
                isNumeric = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amountText.toDoubleOrNull()?.let { onConfirm(Money.fromMajor(it)) } },
                enabled = amountText.toDoubleOrNull() != null
            ) { Text("Add", color = Nova.colors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Nova.colors.textSecondary) }
        }
    )
}
