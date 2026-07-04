package com.novafinance.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

data class NovaDonutSlice(val value: Float, val color: Color)

/**
 * Ring chart for category breakdown. Gaps between slices and rounded caps
 * are drawn via [StrokeCap.Butt] with a small angular gap rather than
 * true rounded caps — rounded caps on a segment this thin would visually
 * overlap its neighbors and misrepresent the proportions.
 */
@Composable
fun NovaDonutChart(
    slices: List<NovaDonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    centerLabel: String? = null,
    centerSubLabel: String? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val surfaceColor = Nova.colors.surface
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val diameter = size.minDimension - strokePx
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val total = slices.sumOf { it.value.toDouble() }.toFloat()

            if (total <= 0f) {
                drawArc(
                    color = surfaceColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                    topLeft = topLeft,
                    size = arcSize
                )
            } else {
                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 3f else 0f
                slices.forEach { slice ->
                    val sweep = (360f * (slice.value / total) - gapDegrees).coerceAtLeast(0f)
                    drawArc(
                        color = slice.color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt),
                        topLeft = topLeft,
                        size = arcSize
                    )
                    startAngle += sweep + gapDegrees
                }
            }
        }

        if (centerLabel != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = centerLabel, style = Nova.typography.titleLarge, color = Nova.colors.textPrimary)
                if (centerSubLabel != null) {
                    Text(text = centerSubLabel, style = Nova.typography.bodySmall, color = Nova.colors.textSecondary)
                }
            }
        }
    }
}
