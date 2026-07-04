package com.novafinance.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaHeroBalanceCard
import com.novafinance.core.designsystem.component.NovaQuickAction
import com.novafinance.core.designsystem.component.NovaQuickActionRow
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.AssistantAction
import com.novafinance.core.domain.model.AssistantActionType
import com.novafinance.core.domain.model.DashboardLayout
import com.novafinance.core.domain.model.DashboardWidgetConfig
import com.novafinance.core.domain.model.DashboardWidgetType
import com.novafinance.core.domain.model.DreamBackground
import com.novafinance.core.domain.model.DreamDashboardData
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.formatted
import com.novafinance.feature.dashboard.widget.AIInsightsWidgetCard
import com.novafinance.feature.dashboard.widget.BalanceForecastWidgetCard
import com.novafinance.feature.dashboard.widget.DebtCoachWidgetCard
import com.novafinance.feature.dashboard.widget.DebtOverviewWidgetCard
import com.novafinance.feature.dashboard.widget.DebtRecoveryWidgetCard
import com.novafinance.feature.dashboard.widget.DebtWeatherWidgetCard
import com.novafinance.feature.dashboard.widget.FinancialOverviewWidgetCard
import com.novafinance.feature.dashboard.widget.ForecastWidgetCard
import com.novafinance.feature.dashboard.widget.GoalWidgetCard
import com.novafinance.feature.dashboard.widget.RecentActivityWidgetCard
import com.novafinance.feature.dashboard.widget.SourceHealthWidgetCard

/**
 * Route-level composable wired into navigation. Delegates to the
 * stateless [DashboardScreen] so the screen itself stays trivially previewable.
 */
@Composable
fun DashboardRoute(
    onAddTransaction: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDashboardStudio: () -> Unit,
    onOpenDebt: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        uiState = uiState,
        onAddTransaction = onAddTransaction,
        onOpenAccounts = onOpenAccounts,
        onOpenBudgets = onOpenBudgets,
        onOpenGoals = onOpenGoals,
        onSeeAllTransactions = onSeeAllTransactions,
        onOpenAssistant = onOpenAssistant,
        onOpenDashboardStudio = onOpenDashboardStudio,
        onOpenDebt = onOpenDebt,
        onAssistantActionClick = { action ->
            when (action.type) {
                AssistantActionType.OPEN_BUDGETS -> onOpenBudgets()
                AssistantActionType.OPEN_GOALS -> onOpenGoals()
                AssistantActionType.OPEN_ANALYTICS -> onOpenAnalytics()
                AssistantActionType.ADD_TRANSACTION -> onAddTransaction()
                AssistantActionType.OPEN_DEBT -> onOpenDebt()
            }
        }
    )
}

@Composable
private fun DashboardScreen(
    uiState: DashboardUiState,
    onAddTransaction: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDashboardStudio: () -> Unit,
    onOpenDebt: () -> Unit,
    onAssistantActionClick: (AssistantAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DreamBackgroundLayer(uiState.layout?.background ?: DreamBackground.None)

        when {
            uiState.isLoading && uiState.data == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Nova.colors.primary
                )
            }

            uiState.data != null && uiState.layout != null -> {
                DashboardContent(
                    data = uiState.data,
                    layout = uiState.layout,
                    onAddTransaction = onAddTransaction,
                    onOpenAccounts = onOpenAccounts,
                    onOpenBudgets = onOpenBudgets,
                    onOpenGoals = onOpenGoals,
                    onSeeAllTransactions = onSeeAllTransactions,
                    onOpenAssistant = onOpenAssistant,
                    onOpenDashboardStudio = onOpenDashboardStudio,
                    onOpenDebt = onOpenDebt,
                    onAssistantActionClick = onAssistantActionClick
                )
            }

            uiState.errorMessage != null -> {
                NovaEmptyState(
                    icon = NovaIcons.Close,
                    title = "Couldn't load your dashboard",
                    message = uiState.errorMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

/**
 * [DreamBackground.None] renders nothing (the ordinary solid
 * background shows through). [DreamBackground.DeviceImage] draws the
 * picked image full-bleed behind everything, with a scrim gradient
 * dark enough at the bottom that widget text stays readable regardless
 * of the image's own brightness — "never reduce readability" from the
 * 9.8 brief is enforced here, not left to chance per-image.
 * [DreamBackground.AiGenerated] has no generator behind it yet (see
 * that case's own doc comment) and intentionally renders as [None].
 */
@Composable
private fun DreamBackgroundLayer(background: DreamBackground) {
    if (background !is DreamBackground.DeviceImage) return

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = background.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Nova.colors.background.copy(alpha = 0.55f),
                            Nova.colors.background.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun DashboardContent(
    data: DreamDashboardData,
    layout: DashboardLayout,
    onAddTransaction: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDashboardStudio: () -> Unit,
    onOpenDebt: () -> Unit,
    onAssistantActionClick: (AssistantAction) -> Unit
) {
    val summary = data.summary
    val visibleWidgets = layout.widgets.filter { it.isVisible }.sortedBy { it.order }
    val goalForecastsById = data.goalForecasts.associateBy { it.goal.id }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Nova.spacing.screenHorizontal,
            vertical = Nova.spacing.screenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.sectionGap)
    ) {
        item {
            NovaHeroBalanceCard(
                label = "Total balance",
                amountText = summary.totalBalance.formatted(),
                deltaText = "${summary.monthIncome.formatted()} in this month",
                isDeltaPositive = true
            )
        }

        item {
            DashboardHeaderRow(onOpenDashboardStudio = onOpenDashboardStudio)
        }

        item {
            NovaQuickActionRow(
                actions = listOf(
                    NovaQuickAction("Add", NovaIcons.Plus, onAddTransaction),
                    NovaQuickAction("Accounts", NovaIcons.Wallet, onOpenAccounts),
                    NovaQuickAction("Budgets", NovaIcons.Target, onOpenBudgets),
                    NovaQuickAction("Goals", NovaIcons.Target, onOpenGoals)
                )
            )
        }

        if (visibleWidgets.isEmpty()) {
            item {
                NovaEmptyState(
                    icon = NovaIcons.Wallet,
                    title = "Your dashboard is empty",
                    message = "Open Dashboard Studio to add widgets, or pick a preset to get started."
                )
            }
        } else {
            items(visibleWidgets, key = { it.id }) { widget ->
                DashboardWidgetDispatcher(
                    widget = widget,
                    data = data,
                    goalForecastsById = goalForecastsById,
                    onOpenAssistant = onOpenAssistant,
                    onAssistantActionClick = onAssistantActionClick,
                    onSeeAllTransactions = onSeeAllTransactions,
                    onOpenDebt = onOpenDebt
                )
            }
        }
    }
}

@Composable
private fun DashboardHeaderRow(onOpenDashboardStudio: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Your dashboard", style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
        IconButton(onClick = onOpenDashboardStudio) {
            Icon(imageVector = NovaIcons.Edit, contentDescription = "Customize dashboard", tint = Nova.colors.textSecondary)
        }
    }
}

/**
 * The one place a [DashboardWidgetConfig] becomes an actual rendered
 * card — every [DashboardWidgetType] branch here, never scattered
 * across the screen. A [DashboardWidgetType.GOAL] widget whose
 * referenced goal was deleted renders nothing rather than crashing;
 * Dashboard Studio's own "goals without widgets" list is what surfaces
 * orphaned/missing associations, not a silent card here.
 */
@Composable
private fun DashboardWidgetDispatcher(
    widget: DashboardWidgetConfig,
    data: DreamDashboardData,
    goalForecastsById: Map<String, GoalForecast>,
    onOpenAssistant: () -> Unit,
    onAssistantActionClick: (AssistantAction) -> Unit,
    onSeeAllTransactions: () -> Unit,
    onOpenDebt: () -> Unit
) {
    when (widget.type) {
        DashboardWidgetType.GOAL -> {
            val forecast = widget.goalId?.let { goalForecastsById[it] } ?: return
            GoalWidgetCard(
                forecast = forecast,
                visualizationMode = widget.visualizationMode,
                size = widget.size,
                // The Goals screen doesn't support deep-linking to one
                // specific goal yet — an explicit no-op here rather
                // than silently wired to the general Goals list, which
                // would look like a bug fix rather than the deliberate
                // gap it is. See DASHBOARD_ARCHITECTURE.md.
                onClick = {}
            )
        }
        DashboardWidgetType.FORECAST -> ForecastWidgetCard(forecast = data.forecast, size = widget.size)
        DashboardWidgetType.FINANCIAL_OVERVIEW -> FinancialOverviewWidgetCard(overview = data.balanceOverview, size = widget.size)
        DashboardWidgetType.AI_INSIGHTS -> {
            if (data.topInsightMessage.isBlank()) return
            AIInsightsWidgetCard(
                message = data.topInsightMessage,
                suggestedAction = data.topInsightAction,
                size = widget.size,
                onLaunchAssistant = onOpenAssistant,
                onActionClick = onAssistantActionClick
            )
        }
        DashboardWidgetType.RECENT_ACTIVITY -> RecentActivityWidgetCard(summary = data.summary, size = widget.size, onSeeAll = onSeeAllTransactions)
        DashboardWidgetType.DEBT_OVERVIEW -> DebtOverviewWidgetCard(summary = data.debtSummary, size = widget.size, onClick = onOpenDebt)
        DashboardWidgetType.DEBT_WEATHER -> DebtWeatherWidgetCard(weather = data.debtSummary.weather, size = widget.size)
        DashboardWidgetType.DEBT_COACH -> DebtCoachWidgetCard(recommendation = data.topDebtRecommendation, size = widget.size, onClick = onOpenDebt)
        DashboardWidgetType.DEBT_RECOVERY -> DebtRecoveryWidgetCard(summary = data.debtSummary, size = widget.size)
        DashboardWidgetType.SOURCE_HEALTH -> SourceHealthWidgetCard(sources = data.sources, healths = data.sourceHealths, size = widget.size)
        DashboardWidgetType.BALANCE_FORECAST -> BalanceForecastWidgetCard(overview = data.balanceOverview, size = widget.size)
    }
}
