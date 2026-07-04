package com.novafinance.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4dp-grid spacing scale. Every margin, gap and padding value in the app
 * should resolve to one of these tokens — no composable should hardcode
 * a raw `.dp` literal for layout spacing (see engineering rule: no
 * hardcoded values).
 */
@Immutable
data class NovaSpacing(
    val none: Dp,
    val xxs: Dp,
    val xs: Dp,
    val sm: Dp,
    val md: Dp,
    val lg: Dp,
    val xl: Dp,
    val xxl: Dp,
    val xxxl: Dp,
    /** Leading/trailing padding for full-bleed screens. */
    val screenHorizontal: Dp,
    /** Top/bottom padding for full-bleed screens. */
    val screenVertical: Dp,
    /** Internal padding for standard content cards. */
    val cardPadding: Dp,
    /** Internal padding for the hero balance card. */
    val heroCardPadding: Dp,
    /** Vertical gap between distinct sections on a screen. */
    val sectionGap: Dp,
    /** Gap between items inside a horizontal quick-action row. */
    val quickActionGap: Dp
)

val NovaDefaultSpacing = NovaSpacing(
    none = 0.dp,
    xxs = 2.dp,
    xs = 4.dp,
    sm = 8.dp,
    md = 12.dp,
    lg = 16.dp,
    xl = 24.dp,
    xxl = 32.dp,
    xxxl = 40.dp,
    screenHorizontal = 20.dp,
    screenVertical = 16.dp,
    cardPadding = 20.dp,
    heroCardPadding = 24.dp,
    sectionGap = 28.dp,
    quickActionGap = 12.dp
)

val LocalNovaSpacing = staticCompositionLocalOf { NovaDefaultSpacing }
