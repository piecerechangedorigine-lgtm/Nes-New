package com.novafinance.core.designsystem

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import com.novafinance.core.domain.model.TransactionCategory

/**
 * [android.graphics.Color.parseColor] does real string parsing on every
 * call. [TransactionCategory.colorHex] is a fixed, small, enum-backed set
 * of values, so parsing each one once and caching the result turns every
 * transaction row, budget card and analytics legend swatch — which used
 * to reparse their category's hex string on every single recomposition —
 * into a plain map lookup.
 *
 * This is also the one place `colorHex` (a plain string, kept in
 * core:domain deliberately free of any Compose dependency — see
 * [TransactionCategory] doc) gets turned into an actual [Color]. Screens
 * should always go through [TransactionCategory.color] rather than
 * parsing [TransactionCategory.colorHex] themselves.
 */
private val categoryColorCache: Map<TransactionCategory, Color> =
    TransactionCategory.entries.associateWith { category ->
        Color(AndroidColor.parseColor(category.colorHex))
    }

val TransactionCategory.color: Color
    get() = categoryColorCache.getValue(this)
