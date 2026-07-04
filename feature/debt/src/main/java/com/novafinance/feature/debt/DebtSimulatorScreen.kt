package com.novafinance.feature.debt

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
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaChipOption
import com.novafinance.core.designsystem.component.NovaChipRow
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaTextField
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.DebtSimulationResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtSimulatorRoute(
    onBack: () -> Unit,
    viewModel: DebtSimulatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DebtSimulatorScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectDebt = viewModel::onSelectDebt,
        onSelectScenarioType = viewModel::onSelectScenarioType,
        onAmountChange = viewModel::onAmountChange,
        onDelayMonthsChange = viewModel::onDelayMonthsChange
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtSimulatorScreen(
    uiState: DebtSimulatorUiState,
    onBack: () -> Unit,
    onSelectDebt: (String) -> Unit,
    onSelectScenarioType: (ScenarioType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelayMonthsChange: (String) -> Unit
) {
    Scaffold(
        containerColor = Nova.colors.background,
        topBar = {
            TopAppBar(
                title = { Text("Debt Simulator") },
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
        }
    ) { scaffoldPadding ->
        if (uiState.owedDebts.isEmpty() && !uiState.isLoading) {
            NovaEmptyState(
                icon = NovaIcons.Target,
                title = "Nothing to simulate yet",
                message = "Add a debt with a monthly payment first.",
                modifier = Modifier.fillMaxSize().background(Nova.colors.background).padding(scaffoldPadding)
            )
        } else {
            DebtSimulatorContent(
                uiState = uiState,
                contentPadding = PaddingValues(
                    horizontal = Nova.spacing.screenHorizontal,
                    vertical = scaffoldPadding.calculateTopPadding() + Nova.spacing.screenVertical
                ),
                onSelectDebt = onSelectDebt,
                onSelectScenarioType = onSelectScenarioType,
                onAmountChange = onAmountChange,
                onDelayMonthsChange = onDelayMonthsChange
            )
        }
    }
}

@Composable
private fun DebtSimulatorContent(
    uiState: DebtSimulatorUiState,
    contentPadding: PaddingValues,
    onSelectDebt: (String) -> Unit,
    onSelectScenarioType: (ScenarioType) -> Unit,
    onAmountChange: (String) -> Unit,
    onDelayMonthsChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Nova.colors.background),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.md)
    ) {
        item {
            Text(
                text = "Nothing here changes your real debts until you decide to act on it yourself.",
                style = Nova.typography.bodySmall,
                color = Nova.colors.textTertiary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                Text(text = "Debt", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = uiState.owedDebts.map { NovaChipOption<String?>(it.id, it.name) },
                    selected = uiState.selectedDebtId,
                    onSelect = { id -> id?.let(onSelectDebt) }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
                Text(text = "Scenario", style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                NovaChipRow(
                    options = listOf(
                        NovaChipOption(ScenarioType.INCREASE_PAYMENT, "Increase payment"),
                        NovaChipOption(ScenarioType.DELAY_PAYMENT, "Delay payment")
                    ),
                    selected = uiState.scenarioType,
                    onSelect = onSelectScenarioType
                )
            }
        }

        item {
            when (uiState.scenarioType) {
                ScenarioType.INCREASE_PAYMENT -> NovaTextField(
                    value = uiState.amountText,
                    onValueChange = { input -> onAmountChange(input.filter { it.isDigit() || it == '.' }) },
                    label = "New monthly payment",
                    isNumeric = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ScenarioType.DELAY_PAYMENT -> NovaTextField(
                    value = uiState.delayMonthsText,
                    onValueChange = { input -> onDelayMonthsChange(input.filter { it.isDigit() }) },
                    label = "Months to delay",
                    isNumeric = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        uiState.result?.let { result ->
            item { SimulationResultCard(result) }

            if (result.goalImpacts.isNotEmpty()) {
                item {
                    Text(text = "Impact on your goals", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
                }
                items(result.goalImpacts, key = { it.goal.id }) { impact ->
                    NovaCard(modifier = Modifier.fillMaxWidth()) {
                        Text(text = impact.message, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimulationResultCard(result: DebtSimulationResult) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Projected impact", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)

        val deltaText = result.freedomDateDeltaMonths?.let { delta ->
            when {
                delta > 0 -> "$delta month${if (delta == 1) "" else "s"} sooner"
                delta < 0 -> "${-delta} month${if (-delta == 1) "" else "s"} later"
                else -> "No change"
            }
        } ?: "Can't be projected with the current numbers"

        Text(text = "Debt freedom: $deltaText", style = Nova.typography.bodyMedium, color = Nova.colors.textSecondary)

        val baselineDate = result.baseline.debtFreeDate?.toString() ?: "not projectable"
        val scenarioDate = result.scenario.debtFreeDate?.toString() ?: "not projectable"
        Text(text = "Baseline: $baselineDate → Scenario: $scenarioDate", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
    }
}
