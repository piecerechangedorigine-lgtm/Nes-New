package com.novafinance.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val NovaDarkColorScheme = darkColorScheme(
    primary = NovaDarkColors.primary,
    onPrimary = NovaDarkColors.onPrimary,
    primaryContainer = NovaDarkColors.primaryContainer,
    background = NovaDarkColors.background,
    onBackground = NovaDarkColors.textPrimary,
    surface = NovaDarkColors.surface,
    onSurface = NovaDarkColors.textPrimary,
    surfaceVariant = NovaDarkColors.elevatedSurface,
    onSurfaceVariant = NovaDarkColors.textSecondary,
    error = NovaDarkColors.error,
    errorContainer = NovaDarkColors.errorContainer,
    onError = NovaDarkColors.textPrimary,
    outline = NovaDarkColors.border,
    outlineVariant = NovaDarkColors.divider,
    scrim = NovaDarkColors.scrim
)

private fun novaMaterialTypography(typography: NovaTypography) = Typography(
    displayLarge = typography.displayLarge,
    displayMedium = typography.displayMedium,
    headlineLarge = typography.headlineLarge,
    headlineMedium = typography.headlineMedium,
    titleLarge = typography.titleLarge,
    titleMedium = typography.titleMedium,
    titleSmall = typography.titleSmall,
    bodyLarge = typography.bodyLarge,
    bodyMedium = typography.bodyMedium,
    bodySmall = typography.bodySmall,
    labelLarge = typography.labelLarge,
    labelMedium = typography.labelMedium,
    labelSmall = typography.labelSmall
)

/**
 * Root theme for the app. Nova is dark-first by brand — there is no light
 * variant in v1, so [darkTheme] only exists to avoid a hardcoded
 * [MaterialTheme] call and to make a future light palette a one-line addition
 * (swap [NovaDarkColorScheme]/[NovaDarkColors] for a light pair here).
 *
 * Exposes two parallel token systems on purpose:
 *  - `MaterialTheme.colorScheme` / `.typography` / `.shapes` — for stock
 *    Material3 components (Button, Scaffold, TextField) that read theme
 *    values internally and can't be redirected to a custom CompositionLocal.
 *  - `Nova.colors` / `.typography` / `.spacing` / `.shapes` / `.elevation`
 *    — the full brand token set, including roles Material3 has no slot for
 *    (hero gradient, chart palette, numeric type styles, spacing scale).
 * Custom Nova components should always prefer the latter.
 */
@Composable
fun NovaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = NovaDarkColors
    val typography = NovaDefaultTypography
    val spacing = NovaDefaultSpacing
    val shapes = NovaDefaultShapes
    val elevation = NovaDefaultElevation

    CompositionLocalProvider(
        LocalNovaColors provides colors,
        LocalNovaTypography provides typography,
        LocalNovaSpacing provides spacing,
        LocalNovaShapes provides shapes,
        LocalNovaElevation provides elevation
    ) {
        MaterialTheme(
            colorScheme = NovaDarkColorScheme,
            typography = novaMaterialTypography(typography),
            shapes = NovaMaterialShapes,
            content = content
        )
    }
}

/**
 * Central accessor for every Nova design token — see [NovaTheme] doc for
 * when to use this vs. `MaterialTheme`. Usage: `Nova.colors.primary`,
 * `Nova.spacing.lg`, `Nova.typography.displayLarge`, etc.
 */
object Nova {
    val colors: NovaColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaColors.current

    val typography: NovaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaTypography.current

    val spacing: NovaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaSpacing.current

    val shapes: NovaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaShapes.current

    val elevation: NovaElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalNovaElevation.current
}
