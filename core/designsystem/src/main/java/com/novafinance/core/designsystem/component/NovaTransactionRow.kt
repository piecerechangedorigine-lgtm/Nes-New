package com.novafinance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

/**
 * One row of the transaction list / activity feed. Category is rendered
 * as a colored swatch + initial rather than a bespoke icon per category —
 * see TransactionCategory doc in core:domain for why.
 *
 * [onLongClick] defaults to a no-op so read-only contexts (Dashboard's
 * recent-activity feed) don't need to opt into anything; the editable
 * Transactions list wires it to the delete-confirmation flow, with
 * [onClick] opening edit — long-press-to-delete rather than a second
 * trailing icon so the row stays uncluttered next to the amount.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NovaTransactionRow(
    merchant: String,
    categoryLabel: String,
    categoryColor: Color,
    dateText: String,
    amountText: String,
    isIncome: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = Nova.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Nova.spacing.md)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(categoryColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = merchant.take(1).uppercase(),
                style = Nova.typography.titleSmall,
                color = categoryColor
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant,
                style = Nova.typography.bodyLarge,
                color = Nova.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$categoryLabel · $dateText",
                style = Nova.typography.bodySmall,
                color = Nova.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = amountText,
            style = Nova.typography.numericMedium,
            color = if (isIncome) Nova.colors.success else Nova.colors.textPrimary
        )
    }
}
