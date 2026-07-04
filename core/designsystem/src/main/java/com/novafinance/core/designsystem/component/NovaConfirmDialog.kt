package com.novafinance.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.novafinance.core.designsystem.Nova

/**
 * The one confirmation dialog every destructive action in the app should
 * go through — Accounts, Transactions, Budgets, and Goals all reuse this
 * rather than each rolling its own `AlertDialog`, so a person always sees
 * the same shape of "are you sure" regardless of what they're deleting.
 */
@Composable
fun NovaConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Nova.colors.elevatedSurface,
        title = { Text(text = title, style = Nova.typography.titleLarge, color = Nova.colors.textPrimary) },
        text = { Text(text = message, style = Nova.typography.bodyMedium, color = Nova.colors.textSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmLabel, color = Nova.colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = Nova.colors.textSecondary)
            }
        }
    )
}
