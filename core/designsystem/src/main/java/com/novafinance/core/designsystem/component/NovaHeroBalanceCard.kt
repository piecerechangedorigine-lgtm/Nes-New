package com.novafinance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.icons.NovaIcons

/**
 * The single most prominent element on Dashboard and Accounts — total
 * balance at display scale, with a thin hero-gradient accent bar rather
 * than a full gradient fill (the gradient is a signature touch, not
 * wallpaper; using it sparingly is what keeps it premium instead of loud).
 */
@Composable
fun NovaHeroBalanceCard(
    label: String,
    amountText: String,
    deltaText: String?,
    isDeltaPositive: Boolean,
    modifier: Modifier = Modifier
) {
    NovaCard(
        modifier = modifier.fillMaxWidth(),
        shape = Nova.shapes.extraLarge,
        contentPadding = PaddingValues(Nova.spacing.heroCardPadding)
    ) {
        Row(
            modifier = Modifier
                .clip(Nova.shapes.full)
                .background(Nova.colors.heroGradient)
                .size(width = 40.dp, height = 4.dp)
        ) {}

        Spacer(modifier = Modifier.height(Nova.spacing.sm))

        Text(text = label, style = Nova.typography.titleSmall, color = Nova.colors.textSecondary)

        Text(text = amountText, style = Nova.typography.displayLarge, color = Nova.colors.textPrimary)

        if (deltaText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon: ImageVector = if (isDeltaPositive) NovaIcons.ArrowUpRight else NovaIcons.ArrowDownRight
                val tint = if (isDeltaPositive) Nova.colors.success else Nova.colors.error
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = deltaText, style = Nova.typography.numericSmall, color = tint)
            }
        }
    }
}
