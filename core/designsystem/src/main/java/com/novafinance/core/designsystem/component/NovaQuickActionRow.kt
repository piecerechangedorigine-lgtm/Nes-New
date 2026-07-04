package com.novafinance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

data class NovaQuickAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/** Row of icon+label quick actions below the Dashboard hero card — capped at 4 to match the "no overloaded screens" UX rule. */
@Composable
fun NovaQuickActionRow(actions: List<NovaQuickAction>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.quickActionGap)
    ) {
        actions.take(4).forEach { action ->
            NovaQuickActionButton(action = action, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NovaQuickActionButton(action: NovaQuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(Nova.shapes.medium)
            .clickable(onClick = action.onClick)
            .semantics { role = Role.Button }
            .padding(vertical = Nova.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Nova.colors.elevatedSurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = Nova.colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(text = action.label, style = Nova.typography.labelSmall, color = Nova.colors.textSecondary)
    }
}
