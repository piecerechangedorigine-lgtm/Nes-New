package com.novafinance.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.icons.NovaIcons

/** Standard "Section Title    See all >" header used to open every card group on Dashboard/Accounts/Budgets/Goals. */
@Composable
fun NovaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingLabel: String? = null,
    onTrailingClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = Nova.typography.headlineMedium, color = Nova.colors.textPrimary)

        if (trailingLabel != null && onTrailingClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(onClick = onTrailingClick)
            ) {
                Text(text = trailingLabel, style = Nova.typography.labelLarge, color = Nova.colors.textSecondary)
                Icon(
                    imageVector = NovaIcons.ChevronRight,
                    contentDescription = null,
                    tint = Nova.colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
