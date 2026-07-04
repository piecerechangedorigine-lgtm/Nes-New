package com.novafinance.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.color
import com.novafinance.core.designsystem.chart.NovaBarGroup
import com.novafinance.core.designsystem.chart.NovaDonutChart
import com.novafinance.core.designsystem.chart.NovaDonutSlice
import com.novafinance.core.designsystem.chart.NovaGroupedBarChart
import com.novafinance.core.designsystem.chart.NovaLineChart
import com.novafinance.core.designsystem.component.NovaCard
import com.novafinance.core.designsystem.component.NovaEmptyState
import com.novafinance.core.designsystem.component.NovaSectionHeader
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.AnalyticsSummary
import com.novafinance.core.domain.model.CategoryBreakdown
import com.novafinance.core.domain.model.MonthlyTotal
import com.novafinance.core.domain.model.formatted
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Route-level composable wired into navigation. Delegates to the
 * stateless [AnalyticsScreen] so the screen itself stays trivially previewable.
 */
@Composable
fun AnalyticsRoute(
    onOpenAssistant: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AnalyticsScreen(uiState = uiState, onOpenAssistant = onOpenAssistant)
}

@Composable
private fun AnalyticsScreen(uiState: AnalyticsUiState, onOpenAssistant: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Nova.colors.background)
    ) {
        when {
            uiState.isLoading && uiState.summary == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Nova.colors.primary
                )
            }

            uiState.summary != null -> {
                AnalyticsContent(summary = uiState.summary, onOpenAssistant = onOpenAssistant)
            }

            uiState.errorMessage != null -> {
                NovaEmptyState(
                    icon = NovaIcons.Close,
                    title = "Couldn't load analytics",
                    message = uiState.errorMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun AnalyticsContent(summary: AnalyticsSummary, onOpenAssistant: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = Nova.spacing.screenHorizontal,
            vertical = Nova.spacing.screenVertical
        ),
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.sectionGap)
    ) {
        item {
            Text(text = "Analytics", style = Nova.typography.headlineLarge, color = Nova.colors.textPrimary)
        }

        item { NovaSectionHeader(title = "Income vs. expenses") }
        item { MonthlyComparisonCard(summary.monthlyTrend) }

        item { NovaSectionHeader(title = "Where it went") }
        item {
            if (summary.currentMonthBreakdown.isEmpty()) {
                NovaEmptyState(
                    icon = NovaIcons.ChartBars,
                    title = "No spend this month yet",
                    message = "Category breakdown fills in once you log some transactions."
                )
            } else {
                CategoryBreakdownCard(summary.currentMonthBreakdown)
            }
        }

        item { NovaSectionHeader(title = "Spending trend") }
        item { TrendCard(summary.monthlyTrend, summary.monthOverMonthExpenseChangePercent) }

        item { AskNovaCard(onClick = onOpenAssistant) }
    }
}

@Composable
private fun MonthlyComparisonCard(trend: List<MonthlyTotal>) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        NovaGroupedBarChart(
            groups = trend.map {
                NovaBarGroup(
                    label = it.month.format(DateTimeFormatter.ofPattern("MMM")),
                    primaryValue = it.income.toMajorDouble().toFloat(),
                    secondaryValue = it.expense.toMajorDouble().toFloat()
                )
            },
            primaryColor = Nova.colors.success,
            secondaryColor = Nova.colors.error,
            modifier = Modifier.fillMaxWidth()
        )
        LegendRow(
            listOf(
                "Income" to Nova.colors.success,
                "Expenses" to Nova.colors.error
            )
        )
    }
}

@Composable
private fun CategoryBreakdownCard(breakdown: List<CategoryBreakdown>) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NovaDonutChart(
                slices = breakdown.map {
                    NovaDonutSlice(
                        value = it.percentOfTotal,
                        color = it.category.color
                    )
                },
                centerLabel = breakdown.firstOrNull()?.let { "${(it.percentOfTotal * 100).toInt()}%" },
                centerSubLabel = breakdown.firstOrNull()?.category?.displayName,
                modifier = Modifier.size(120.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Nova.spacing.md),
                verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)
            ) {
                breakdown.take(5).forEach { item ->
                    CategoryLegendRow(item)
                }
            }
        }
    }
}

@Composable
private fun CategoryLegendRow(item: CategoryBreakdown) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(Nova.shapes.full)
                    .background(item.category.color)
            )
            Text(
                text = item.category.displayName,
                style = Nova.typography.bodySmall,
                color = Nova.colors.textPrimary,
                modifier = Modifier.padding(start = Nova.spacing.xs)
            )
        }
        Text(text = item.amount.formatted(), style = Nova.typography.labelMedium, color = Nova.colors.textSecondary)
    }
}

@Composable
private fun TrendCard(trend: List<MonthlyTotal>, monthOverMonthChangePercent: Float?) {
    NovaCard(modifier = Modifier.fillMaxWidth()) {
        NovaLineChart(
            values = trend.map { it.expense.toMajorDouble().toFloat() },
            color = Nova.colors.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        )
        if (monthOverMonthChangePercent != null) {
            val isIncrease = monthOverMonthChangePercent > 0
            val tint = if (isIncrease) Nova.colors.warning else Nova.colors.success
            Text(
                text = "Expenses ${if (isIncrease) "up" else "down"} ${abs(monthOverMonthChangePercent).toInt()}% vs. last month",
                style = Nova.typography.bodySmall,
                color = tint
            )
        }
    }
}

@Composable
private fun AskNovaCard(onClick: () -> Unit) {
    NovaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Nova.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Ask Nova", style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
                Text(
                    text = "Get plain-language answers about your spending",
                    style = Nova.typography.bodySmall,
                    color = Nova.colors.textSecondary
                )
            }
            Icon(
                imageVector = NovaIcons.ChevronRight,
                contentDescription = null,
                tint = Nova.colors.textSecondary
            )
        }
    }
}

@Composable
private fun LegendRow(entries: List<Pair<String, Color>>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.lg)
    ) {
        entries.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(Nova.shapes.full)
                        .background(color)
                )
                Text(
                    text = label,
                    style = Nova.typography.labelSmall,
                    color = Nova.colors.textSecondary,
                    modifier = Modifier.padding(start = Nova.spacing.xs)
                )
            }
        }
    }
}
