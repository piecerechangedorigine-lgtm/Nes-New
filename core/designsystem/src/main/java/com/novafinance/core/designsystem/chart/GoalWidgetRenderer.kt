package com.novafinance.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.component.NovaLinearProgressBar
import com.novafinance.core.designsystem.icons.NovaIcons
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.GoalHealthLabel
import com.novafinance.core.domain.model.GoalVisualizationMode
import com.novafinance.core.domain.model.Money
import com.novafinance.core.domain.model.formatted
import com.novafinance.core.domain.model.health
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * The single dispatch point every [GoalVisualizationMode] renders
 * through. Every mode reads the exact same [GoalForecast] (plus the
 * [com.novafinance.core.domain.model.GoalHealth] computed from it) — a
 * sixth mode is purely a new `when` branch here, never a data-layer
 * change. See `DASHBOARD_ARCHITECTURE.md` for the full rationale.
 */
@Composable
fun GoalWidgetVisualization(
    forecast: GoalForecast,
    mode: GoalVisualizationMode,
    modifier: Modifier = Modifier
) {
    when (mode) {
        GoalVisualizationMode.RING -> RingVisualization(forecast, modifier)
        GoalVisualizationMode.TIMELINE -> TimelineVisualization(forecast, modifier)
        GoalVisualizationMode.HORIZON -> HorizonVisualization(forecast, modifier)
        GoalVisualizationMode.VELOCITY -> VelocityVisualization(forecast, modifier)
        GoalVisualizationMode.WEATHER -> WeatherVisualization(forecast, modifier)
    }
}

@Composable
private fun RingVisualization(forecast: GoalForecast, modifier: Modifier) {
    val goal = forecast.goal
    val surfaceColor = Nova.colors.surface
    val primaryColor = Nova.colors.primary
    Box(modifier = modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            val strokePx = 14.dp.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = surfaceColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = 360f * goal.percentComplete.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
                topLeft = topLeft,
                size = arcSize
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${(goal.percentComplete * 100).roundToInt()}%", style = Nova.typography.titleLarge, color = Nova.colors.textPrimary)
            Text(text = goal.name, style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
        }
    }
}

@Composable
private fun TimelineVisualization(forecast: GoalForecast, modifier: Modifier) {
    val goal = forecast.goal
    val surfaceColor = Nova.colors.surface
    val primaryColor = Nova.colors.primary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
        Text(text = goal.name, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
        NovaLinearProgressBar(progress = goal.percentComplete, color = primaryColor, modifier = Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = goal.createdAt.toString(), style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
            Text(
                text = goal.targetDate?.toString() ?: "No deadline",
                style = Nova.typography.labelSmall,
                color = Nova.colors.textTertiary
            )
        }
    }
}

/**
 * Progress as a rising fill against a horizon baseline — a simple
 * bar-chart metaphor ("how close to the top") rather than a literal
 * skyline illustration, which would need real artwork this component
 * doesn't have.
 */
@Composable
private fun HorizonVisualization(forecast: GoalForecast, modifier: Modifier) {
    val goal = forecast.goal
    val surfaceColor = Nova.colors.surface
    val primaryColor = Nova.colors.primary
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
        Text(text = goal.name, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                val fillHeight = size.height * goal.percentComplete.coerceIn(0f, 1f)
                drawRect(color = surfaceColor)
                drawRect(
                    color = primaryColor.copy(alpha = 0.85f),
                    topLeft = Offset(0f, size.height - fillHeight),
                    size = Size(size.width, fillHeight)
                )
                // The horizon line itself, at the fill's leading edge.
                drawLine(
                    color = primaryColor,
                    start = Offset(0f, size.height - fillHeight),
                    end = Offset(size.width, size.height - fillHeight),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        Text(
            text = "${goal.currentAmount.formatted()} of ${goal.targetAmount.formatted()}",
            style = Nova.typography.bodySmall,
            color = Nova.colors.textSecondary
        )
    }
}

/**
 * "Your pace" (actual average monthly saving since the goal was
 * created) against "Needed pace" (`GoalForecast.requiredMonthlyContribution`)
 * as two comparative bars, normalized against whichever is larger — a
 * plain linear-scale comparison rather than a true speedometer gauge,
 * which would need a shape this design system has no other use for.
 */
@Composable
private fun VelocityVisualization(forecast: GoalForecast, modifier: Modifier) {
    val goal = forecast.goal
    val surfaceColor = Nova.colors.surface
    val primaryColor = Nova.colors.primary
    val monthsElapsed = ChronoUnit.MONTHS.between(goal.createdAt, LocalDate.now()).coerceAtLeast(1)
    val actualMonthlyPace = Money(goal.currentAmount.minorUnits / monthsElapsed)
    val neededPace = forecast.requiredMonthlyContribution

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm)) {
        Text(text = goal.name, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)

        if (neededPace == null) {
            Text(text = "No deadline set — pace comparison needs a target date.", style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
        } else {
            val larger = maxOf(actualMonthlyPace.minorUnits, neededPace.minorUnits, 1L)
            VelocityBar(label = "Your pace", amount = actualMonthlyPace, fraction = actualMonthlyPace.minorUnits.toFloat() / larger, color = primaryColor)
            VelocityBar(label = "Needed pace", amount = neededPace, fraction = neededPace.minorUnits.toFloat() / larger, color = Nova.colors.textSecondary)
        }
    }
}

@Composable
private fun VelocityBar(label: String, amount: Money, fraction: Float, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
            Text(text = "${amount.formatted()}/mo", style = Nova.typography.labelSmall, color = Nova.colors.textPrimary)
        }
        NovaLinearProgressBar(progress = fraction.coerceIn(0f, 1f), color = color, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * [com.novafinance.core.domain.model.GoalHealthLabel] expressed as
 * weather — the one visualization mode that's a direct, literal read of
 * Goal Health (Phase 8.5.7) rather than a fresh computation over
 * [GoalForecast].
 */
@Composable
private fun WeatherVisualization(forecast: GoalForecast, modifier: Modifier) {
    val health = forecast.health()
    val (icon, label, tint) = when (health.label) {
        GoalHealthLabel.EXCELLENT -> Triple(NovaIcons.Sun, "Sunny", Nova.colors.success)
        GoalHealthLabel.GOOD -> Triple(NovaIcons.Sun, "Mostly sunny", Nova.colors.success)
        GoalHealthLabel.AT_RISK -> Triple(NovaIcons.Cloud, "Cloudy", Nova.colors.warning)
        GoalHealthLabel.CRITICAL -> Triple(NovaIcons.Storm, "Stormy", Nova.colors.error)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(40.dp))
        Text(text = label, style = Nova.typography.titleMedium, color = tint)
        Text(text = forecast.goal.name, style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
        Text(text = "Health score: ${health.score}/100", style = Nova.typography.labelSmall, color = Nova.colors.textTertiary)
    }
}
