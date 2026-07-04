package com.novafinance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.novafinance.core.designsystem.Nova

/** One selectable pill in a [NovaChipRow]. */
data class NovaChipOption<T>(val value: T, val label: String)

/**
 * Single-select horizontal chip row — the standard way to pick an account
 * type, transaction category, or similar closed set anywhere in a form.
 */
@Composable
fun <T> NovaChipRow(
    options: List<NovaChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.sm)
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            Text(
                text = option.label,
                style = Nova.typography.labelLarge,
                color = if (isSelected) Nova.colors.onPrimary else Nova.colors.textSecondary,
                modifier = Modifier
                    .clip(Nova.shapes.full)
                    .background(if (isSelected) Nova.colors.primary else Nova.colors.elevatedSurfaceHigh)
                    .clickable { onSelect(option.value) }
                    .padding(horizontal = Nova.spacing.lg, vertical = Nova.spacing.sm)
            )
        }
    }
}
