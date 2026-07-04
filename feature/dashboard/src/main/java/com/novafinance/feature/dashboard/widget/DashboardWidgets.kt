package com.novafinance.feature.dashboard.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.chart.GoalWidgetVisualization
import com.novafinance.core.designsystem.color
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaTransactionRow
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.AssistantAction
import com.novafinance.core.domain.model.BalanceOverview
import com.novafinance.core.domain.model.DashboardSummary
import com.novafinance.core.domain.model.DebtHealthLabel
import com.novafinance.core.domain.model.DebtSummary
import com.novafinance.core.domain.model.DebtTrend
import com.novafinance.core.domain.model.DebtWeather
import com.novafinance.core.domain.model.DebtWeatherState
import com.novafinance.core.domain.model.FinancialSource
import com.novafinance.core.domain.model.ForecastConfidence
import com.novafinance.core.domain.model.ForecastStatus
import com.novafinance.core.domain.model.ForecastSummary
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.GoalVisualizationMode
import com.novafinance.core.domain.model.SourceHealth
import com.novafinance.core.domain.model.Transaction
import com.novafinance.core.domain.model.WidgetSize
import com.novafinance.core.domain.model.formatted
import com.novafinance.core.domain.model.formattedWithSign
import com.novafinance.core.domain.model.health

/**
 * Every widget card takes a [WidgetSize] and, per `DashboardWidget.kt`'s
 * own doc comment, currently renders its MEDIUM layout regardless of
 * the value — the parameter is threaded through end-to-end (persisted,
 * editable in Dashboard Studio, passed all the way down to here) so
 * that wiring up real per-size layouts later is purely additive to
 * these composables, never a data-flow change. See
 * DASHBOARD_ARCHITECTURE.md.
 */

@Composable
fun GoalWidgetCard(
    forecast: GoalForecast,
    visualizationMode: GoalVisualizationMode,
    size: WidgetSize,
    onClick: () -> Unit
) {
    val goal = forecast.goal
    val health = forecast.health()

    NovaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        GoalWidgetVisualization(
            forecast = forecast,
            mode = visualizationMode,
            modifier = Modifier.fillMaxWidth().height(if (size == WidgetSize.LARGE) 180.dp else 120.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "${goal.currentAmount.formatted()} saved", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
            Text(text = "${goal.remaining.formatted()} to go", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = forecast.monthsRemaining?.let { "$it month${if (it == 1) "" else "s"} left" } ?: "No deadline",
                style = Nova.typography.labelSmall,
                color = Nova.colors.textTertiary
            )
            Text(text = "Health ${health.score}/100", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
        }
    }
}

@Composable
fun ForecastWidgetCard(forecast: ForecastSummary, size: WidgetSize) {
    val tint = when (forecast.status) {
        ForecastStatus.SURPLUS -> Nova.colors.success
        ForecastStatus.DEFICIT -> Nova.colors.error
        ForecastStatus.ON_TRACK -> Nova.colors.textSecondary
    }

    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
            Icon(imageVector = NovaIcons.Target, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(text = "This month's forecast", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        }
        Text(text = forecast.message, style = Nova.typography.bodyMedium, color = tint)
        if (size != WidgetSize.SMALL) {
            Text(
                text = "Projected end-of-month balance: ${forecast.projectedEndOfMonthBalance.formatted()}",
                style = Nova.typography.bodySmall,
                color = Nova.colors.textSecondary
            )
            Text(
                text = "Confidence: ${confidenceLabel(forecast.confidence)}",
                style = Nova.typography.labelSmall,
                color = Nova.colors.textTertiary
            )
        }
    }
}

private fun confidenceLabel(confidence: ForecastConfidence): String = when (confidence) {
    ForecastConfidence.LOW -> "Low — early in the month"
    ForecastConfidence.MEDIUM -> "Medium"
    ForecastConfidence.HIGH -> "High"
}

@Composable
fun FinancialOverviewWidgetCard(overview: BalanceOverview, size: WidgetSize) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Financial overview", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        OverviewRow(label = "Total liquidity", amount = overview.totalLiquidity.formatted())
        OverviewRow(label = "Available spending power", amount = overview.availableSpendingPower.formatted())
        if (size != WidgetSize.SMALL) {
            OverviewRow(label = "Dream safe balance", amount = overview.dreamSafeBalance.formatted())
        }
    }
}

@Composable
private fun OverviewRow(label: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        Text(text = amount, style = Nova.typography.numericSmall, color = Nova.colors.textPrimary)
    }
}

@Composable
fun AIInsightsWidgetCard(
    message: String,
    suggestedAction: AssistantAction?,
    size: WidgetSize,
    onLaunchAssistant: () -> Unit,
    onActionClick: (AssistantAction) -> Unit
) {
    NovaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onLaunchAssistant)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
            Icon(imageVector = NovaIcons.Sparkle, contentDescription = null, tint = Nova.colors.primary, modifier = Modifier.size(16.dp))
            Text(text = "AI insight", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        }
        Text(text = message, style = Nova.typography.bodyMedium, color = Nova.colors.textSecondary)
        if (suggestedAction != null && size != WidgetSize.SMALL) {
            Text(
                text = suggestedAction.label,
                style = Nova.typography.labelLarge,
                color = Nova.colors.primary,
                modifier = Modifier
                    .clip(Nova.shapes.full)
                    .background(Nova.colors.primaryContainer)
                    .clickable { onActionClick(suggestedAction) }
                    .padding(Nova.spacing.sm)
            )
        }
    }
}

@Composable
fun RecentActivityWidgetCard(summary: DashboardSummary, size: WidgetSize, onSeeAll: () -> Unit) {
    val visibleCount = if (size == WidgetSize.SMALL) 3 else 5
    NovaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onSeeAll)) {
        Text(text = "Recent activity", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        if (summary.recentTransactions.isEmpty()) {
            Text(text = "No transactions yet.", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        } else {
            Column {
                summary.recentTransactions.take(visibleCount).forEach { transaction ->
                    RecentActivityRow(transaction)
                }
            }
        }
    }
}

@Composable
private fun RecentActivityRow(transaction: Transaction) {
    NovaTransactionRow(
        merchant = transaction.merchant,
        categoryLabel = transaction.category.displayName,
        categoryColor = transaction.category.color,
        dateText = transaction.date.toString(),
        amountText = transaction.amount.formattedWithSign(),
        isIncome = transaction.amount.isPositive,
        onClick = {}
    )
}

/** Total debt, Debt Health, and Debt Freedom Date (10.11) — the debt equivalent of [FinancialOverviewWidgetCard]. */
@Composable
fun DebtOverviewWidgetCard(summary: DebtSummary, size: WidgetSize, onClick: () -> Unit) {
    NovaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
            Icon(imageVector = NovaIcons.Wallet, contentDescription = null, tint = healthTint(summary.health.label), modifier = Modifier.size(18.dp))
            Text(text = "Debt overview", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        }
        OverviewRow(label = "Total owed", amount = summary.totalOwed.formatted())
        if (size != WidgetSize.SMALL) {
            OverviewRow(label = "Debt health", amount = "${summary.health.score}/100 · ${summary.health.label.name.lowercase().replace('_', ' ')}")
            val freedomText = summary.freedomProjection.debtFreeDate?.toString() ?: "Not projectable yet"
            OverviewRow(label = "Debt-free by", amount = freedomText)
        }
    }
}

/** Weather state and trend (10.4/10.11) — a glance-level read of debt health, matching the Weather visualization mode's own iconography for consistency. */
@Composable
fun DebtWeatherWidgetCard(weather: DebtWeather, size: WidgetSize) {
    val (icon, label, tint) = when (weather.state) {
        DebtWeatherState.SUNNY -> Triple(NovaIcons.Sun, "Sunny", Nova.colors.success)
        DebtWeatherState.PARTLY_CLOUDY -> Triple(NovaIcons.PartlyCloudy, "Partly cloudy", Nova.colors.success)
        DebtWeatherState.CLOUDY -> Triple(NovaIcons.Cloud, "Cloudy", Nova.colors.warning)
        DebtWeatherState.RAINY -> Triple(NovaIcons.Rain, "Rainy", Nova.colors.warning)
        DebtWeatherState.STORM -> Triple(NovaIcons.Storm, "Stormy", Nova.colors.error)
    }

    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Column {
                Text(text = "Debt weather", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
                Text(text = label, style = Nova.typography.bodyMedium, color = tint)
            }
        }
        if (size != WidgetSize.SMALL) {
            Text(text = "Trend: ${trendLabel(weather.trend)}", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
        }
    }
}

private fun trendLabel(trend: DebtTrend): String = when (trend) {
    DebtTrend.IMPROVING -> "Improving"
    DebtTrend.STABLE -> "Stable"
    DebtTrend.WORSENING -> "Worsening"
}

@Composable
private fun healthTint(label: DebtHealthLabel) = when (label) {
    DebtHealthLabel.HEALTHY -> Nova.colors.success
    DebtHealthLabel.MODERATE -> Nova.colors.primary
    DebtHealthLabel.HIGH_RISK -> Nova.colors.warning
    DebtHealthLabel.CRITICAL -> Nova.colors.error
}

/** Top recommendation from the AI Debt Coach (10.10/10.11) — reuses [com.novafinance.core.domain.assistant.AssistantInsightEngine.topDebtRecommendation] exactly, never a separate message. */
@Composable
fun DebtCoachWidgetCard(recommendation: String?, size: WidgetSize, onClick: () -> Unit) {
    NovaCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
            Icon(imageVector = NovaIcons.Sparkle, contentDescription = null, tint = Nova.colors.primary, modifier = Modifier.size(16.dp))
            Text(text = "Debt coach", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        }
        Text(
            text = recommendation ?: "No debt to coach you through right now.",
            style = Nova.typography.bodyMedium,
            color = Nova.colors.textSecondary
        )
    }
}

/** Top healthy source and top risk source (11.11) — reads [DreamDashboardData.sourceHealths], never a second health computation. */
@Composable
fun SourceHealthWidgetCard(sources: List<FinancialSource>, healths: List<SourceHealth>, size: WidgetSize) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Source health", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        if (healths.isEmpty()) {
            Text(text = "No active sources yet.", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        } else {
            val byId = sources.associateBy { it.id }
            val healthiest = healths.maxByOrNull { it.score }
            val riskiest = healths.minByOrNull { it.score }

            healthiest?.let { SourceHealthRow(label = "Top healthy", name = byId[it.sourceId]?.name.orEmpty(), health = it) }
            if (size != WidgetSize.SMALL && riskiest != null && riskiest.sourceId != healthiest?.sourceId) {
                SourceHealthRow(label = "Needs attention", name = byId[riskiest.sourceId]?.name.orEmpty(), health = riskiest)
            }
        }
    }
}

@Composable
private fun SourceHealthRow(label: String, name: String, health: SourceHealth) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column {
            Text(text = label, style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
            Text(text = name, style = Nova.typography.bodyMedium, color = Nova.colors.textPrimary)
        }
        Text(text = "${health.score}/100", style = Nova.typography.numericSmall, color = healthTint(health.label))
    }
}

/** Month-end projection across forecast-eligible sources only (11.4/11.11) — distinct from the whole-account [ForecastWidgetCard], see `FINANCIAL_SOURCES_ARCHITECTURE.md`. */
@Composable
fun BalanceForecastWidgetCard(overview: BalanceOverview, size: WidgetSize) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Balance forecast", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        Text(text = overview.forecastBalance.formatted(), style = Nova.typography.numericMedium, color = Nova.colors.textPrimary)
        if (size != WidgetSize.SMALL) {
            Text(
                text = "Projected across your forecast-eligible sources by month end.",
                style = Nova.typography.labelSmall,
                color = Nova.colors.textTertiary
            )
        }
    }
}

/** Money owed to the person and expected recovery (10.11) — the receivables side, deliberately never mixed into [DebtOverviewWidgetCard], which is scoped to what's owed. */
@Composable
fun DebtRecoveryWidgetCard(summary: DebtSummary, size: WidgetSize) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
            Icon(imageVector = NovaIcons.Wallet, contentDescription = null, tint = Nova.colors.success, modifier = Modifier.size(18.dp))
            Text(text = "Debt recovery", style = Nova.typography.titleSmall, color = Nova.colors.textPrimary)
        }
        if (summary.activeReceivables.isEmpty()) {
            Text(text = "No outstanding loans to others.", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        } else {
            OverviewRow(label = "Owed to you", amount = summary.totalOwedToMe.formatted())
            if (size != WidgetSize.SMALL) {
                val nextRecovery = summary.activeReceivables.mapNotNull { it.dueDate }.minOrNull()
                OverviewRow(label = "Next expected", amount = nextRecovery?.toString() ?: "No date set")
            }
        }
    }
}
