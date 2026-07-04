package com.novafinance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

/**
 * Category swatch + spent/limit progress bar — the core unit of the
 * Budgets screen. [accentColor] comes from TransactionCategory.colorHex
 * in the domain layer; this component only draws it, never decides it.
 */
@Composable
fun NovaProgressCard(
    title: String,
    subtitle: String,
    progress: Float,
    accentColor: Color,
    isOverLimit: Boolean,
    modifier: Modifier = Modifier
) {
    NovaCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(Nova.spacing.sm))
            Text(
                text = title,
                style = Nova.typography.titleMedium,
                color = Nova.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = subtitle,
            style = Nova.typography.bodySmall,
            color = if (isOverLimit) Nova.colors.error else Nova.colors.textSecondary
        )
        NovaLinearProgressBar(
            progress = progress,
            color = if (isOverLimit) Nova.colors.error else accentColor
        )
    }
}

@Composable
fun NovaLinearProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(Nova.shapes.full)
            .background(Nova.colors.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(Nova.shapes.full)
                .background(color)
        )
    }
}
