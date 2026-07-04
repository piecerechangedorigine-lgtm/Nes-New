package com.novafinance.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

/** Shown wherever a list can legitimately be empty (new account, no transactions yet) instead of a blank screen. */
@Composable
fun NovaEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Nova.spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Nova.colors.textTertiary,
            modifier = Modifier.size(32.dp)
        )
        Text(text = title, style = Nova.typography.titleMedium, color = Nova.colors.textPrimary)
        Text(
            text = message,
            style = Nova.typography.bodyMedium,
            color = Nova.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}
