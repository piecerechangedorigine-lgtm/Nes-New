package com.novafinance.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.novafinance.core.designsystem.Nova

/** Primary call-to-action button — full width, pill shape, indigo fill. Used for every form's submit action. */
@Composable
fun NovaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Nova.shapes.full,
        colors = ButtonDefaults.buttonColors(
            containerColor = Nova.colors.primary,
            contentColor = Nova.colors.onPrimary,
            disabledContainerColor = Nova.colors.elevatedSurfaceHigh,
            disabledContentColor = Nova.colors.textDisabled
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, style = Nova.typography.titleMedium)
    }
}

/** Secondary action button — outline only, for "Cancel" and lower-emphasis actions next to a primary button. */
@Composable
fun NovaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = Nova.shapes.full,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Nova.colors.textPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, style = Nova.typography.titleMedium)
    }
}
