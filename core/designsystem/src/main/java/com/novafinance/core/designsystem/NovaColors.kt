package com.novafinance.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette. Nothing in this object carries semantic meaning —
 * it is the literal set of values from the Nova color spec. Screens and
 * components should never reference [NovaColors] directly; they should
 * read [Nova.colors], which maps these raw values onto roles
 * (surface, content, feedback, etc.) via [NovaColorScheme].
 */
object NovaColors {
    val Background = Color(0xFF0B0B10)
    val Surface = Color(0xFF16161C)
    val Elevated = Color(0xFF1F1F27)
    val ElevatedHigh = Color(0xFF262631)

    val Primary = Color(0xFF5B6CFF)
    val PrimaryContainer = Color(0xFF2A2E66)
    val Success = Color(0xFF3DD68C)
    val SuccessContainer = Color(0xFF16332A)
    val Warning = Color(0xFFF5A623)
    val WarningContainer = Color(0xFF3A2E16)
    val Error = Color(0xFFFF5C5C)
    val ErrorContainer = Color(0xFF3A1B1E)

    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFF8C8C96)
    val TextTertiary = Color(0xFF5E5E68)
    val TextDisabled = Color(0xFF44444C)

    val Border = Color(0xFF2A2A33)
    val Divider = Color(0xFF1F1F27)
    val Scrim = Color(0xCC000000)

    val HeroGradientStart = Color(0xFFF6B8C4)
    val HeroGradientEnd = Color(0xFF9FE8D6)

    /** Fixed hue rotation for analytics charts. Order matters — series are assigned in sequence. */
    val ChartPalette = listOf(
        Primary,
        Color(0xFF9FE8D6),
        Color(0xFFF6B8C4),
        Warning,
        Color(0xFF8C8CFF),
        TextSecondary
    )
}

/**
 * Semantic color roles consumed by screens via `Nova.colors.*`.
 * Splitting this from [NovaColors] means a future light theme only
 * requires a second [NovaColorScheme] instance — no call sites change.
 */
@Immutable
data class NovaColorScheme(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val elevatedSurfaceHigh: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,
    val border: Color,
    val divider: Color,
    val scrim: Color,
    val heroGradient: Brush,
    val chartPalette: List<Color>
)

val NovaDarkColors = NovaColorScheme(
    background = NovaColors.Background,
    surface = NovaColors.Surface,
    elevatedSurface = NovaColors.Elevated,
    elevatedSurfaceHigh = NovaColors.ElevatedHigh,
    primary = NovaColors.Primary,
    onPrimary = NovaColors.TextPrimary,
    primaryContainer = NovaColors.PrimaryContainer,
    success = NovaColors.Success,
    onSuccessContainer = NovaColors.TextPrimary,
    successContainer = NovaColors.SuccessContainer,
    warning = NovaColors.Warning,
    warningContainer = NovaColors.WarningContainer,
    error = NovaColors.Error,
    errorContainer = NovaColors.ErrorContainer,
    textPrimary = NovaColors.TextPrimary,
    textSecondary = NovaColors.TextSecondary,
    textTertiary = NovaColors.TextTertiary,
    textDisabled = NovaColors.TextDisabled,
    border = NovaColors.Border,
    divider = NovaColors.Divider,
    scrim = NovaColors.Scrim,
    heroGradient = Brush.linearGradient(
        colors = listOf(NovaColors.HeroGradientStart, NovaColors.HeroGradientEnd)
    ),
    chartPalette = NovaColors.ChartPalette
)

val LocalNovaColors = staticCompositionLocalOf { NovaDarkColors }
