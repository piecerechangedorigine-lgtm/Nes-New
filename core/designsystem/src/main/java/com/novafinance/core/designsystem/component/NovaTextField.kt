package com.novafinance.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.novafinance.core.designsystem.Nova

/** Standard text input for every form in the app (add account/transaction/budget/goal). */
@Composable
fun NovaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isNumeric: Boolean = false,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isNumeric) KeyboardType.Decimal else KeyboardType.Text
        ),
        shape = Nova.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Nova.colors.primary,
            unfocusedBorderColor = Nova.colors.border,
            focusedTextColor = Nova.colors.textPrimary,
            unfocusedTextColor = Nova.colors.textPrimary,
            focusedLabelColor = Nova.colors.primary,
            unfocusedLabelColor = Nova.colors.textSecondary,
            cursorColor = Nova.colors.primary
        ),
        modifier = modifier
    )
}
