package com.novafinance.core.designsystem.chart

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Single-series trend line — used for the expense-over-time trend on
 * Analytics. Values are plotted against their own min/max range (not a
 * fixed scale), so a flat few-hundred-dollar month and a steep
 * few-thousand-dollar month both use the chart's full height.
 */
@Composable
fun NovaLineChart(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val maxValue = values.max().coerceAtLeast(0.01f)
        val minValue = values.min().coerceAtMost(maxValue - 0.01f)
        val range = (maxValue - minValue).coerceAtLeast(0.01f)
        val stepX = size.width / (values.size - 1)

        fun pointFor(index: Int, value: Float): Offset {
            val x = index * stepX
            val y = size.height - ((value - minValue) / range) * size.height
            return Offset(x, y)
        }

        val path = Path()
        values.forEachIndexed { index, value ->
            val point = pointFor(index, value)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        values.forEachIndexed { index, value ->
            drawCircle(color = color, radius = 4.dp.toPx(), center = pointFor(index, value))
        }
    }
}
