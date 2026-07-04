package com.novafinance.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * Shadow-elevation tokens. On the dark Nova background a drop shadow alone
 * reads as almost invisible, so elevated Nova surfaces are communicated
 * primarily through [NovaColorScheme.elevatedSurface] tone rather than
 * shadow depth — these dp values still drive real shadow on components
 * that need it (sheets, dropdown menus, snackbars).
 */
@Immutable
data class NovaElevation(
    val level0: Dp,
    val level1: Dp,
    val level2: Dp,
    val level3: Dp,
    val level4: Dp,
    val level5: Dp
)

val NovaDefaultElevation = NovaElevation(
    level0 = 0.dp,
    level1 = 1.dp,
    level2 = 3.dp,
    level3 = 6.dp,
    level4 = 8.dp,
    level5 = 12.dp
)

val LocalNovaElevation = staticCompositionLocalOf { NovaDefaultElevation }

/**
 * Blends [NovaColors.Primary] over a base [surface] color, following the
 * Material dark-theme elevation-overlay formula, so cards stacked above
 * one another (e.g. a modal sheet over the dashboard) read as distinct
 * without relying purely on the fixed `Elevated` palette token.
 */
fun surfaceColorAtElevation(surface: Color, elevation: Dp, tint: Color = NovaColors.Primary): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln((elevation.value) + 1)) + 2f) / 100f
    return tint.copy(alpha = alpha.coerceIn(0f, 1f)).compositeOver(surface)
}
