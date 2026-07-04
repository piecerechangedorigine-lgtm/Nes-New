package com.novafinance.core.designsystem.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

data class NovaBarGroup(val label: String, val primaryValue: Float, val secondaryValue: Float)

private val barTopShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)

/**
 * Two-series bar chart (income vs. expense per month). Deliberately built
 * from `fillMaxHeight(fraction)` boxes rather than Canvas — bars with no
 * curves or overlap don't need pixel-level drawing, and staying in the
 * layout system means these bars respond to font-scale/window-size
 * changes the same way the rest of the screen does.
 */
@Composable
fun NovaGroupedBarChart(
    groups: List<NovaBarGroup>,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    barAreaHeight: Dp = 140.dp
) {
    val maxValue = (groups.maxOfOrNull { maxOf(it.primaryValue, it.secondaryValue) } ?: 0f).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeight),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            groups.forEach { group ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(fraction = (group.primaryValue / maxValue).coerceIn(0.02f, 1f))
                            .background(primaryColor, barTopShape)
                    )
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight(fraction = (group.secondaryValue / maxValue).coerceIn(0.02f, 1f))
                            .background(secondaryColor, barTopShape)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Nova.spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            groups.forEach { group ->
                Text(text = group.label, style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
            }
        }
    }
}
