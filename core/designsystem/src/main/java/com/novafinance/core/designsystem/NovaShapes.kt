package com.novafinance.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Corner radii for the Nova component set. Values are deliberately large
 * relative to stock Material — the brand calls for soft, generous rounding
 * on cards and sheets rather than sharp or fully-pill shapes everywhere.
 */
@Immutable
data class NovaShapes(
    /** Chips, small badges, input fields. */
    val small: CornerBasedShape,
    /** Standard list rows, secondary cards, dialogs. */
    val medium: CornerBasedShape,
    /** Primary content cards — quick action tiles, transaction groups. */
    val large: CornerBasedShape,
    /** Hero balance card, featured widgets. */
    val extraLarge: CornerBasedShape,
    /** Bottom sheets and modals — top corners only. */
    val sheet: Shape,
    /** Fully rounded — pill buttons, avatars, status dots. */
    val full: Shape
)

val NovaDefaultShapes = NovaShapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
    sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    full = RoundedCornerShape(percent = 50)
)

/**
 * Material3 components (Button, Card, etc.) read shape off `MaterialTheme.shapes`,
 * so this mirrors [NovaDefaultShapes] into the M3 scale for interop. Custom
 * Nova components should prefer `Nova.shapes.*` directly.
 */
val NovaMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = NovaDefaultShapes.small,
    medium = NovaDefaultShapes.medium,
    large = NovaDefaultShapes.large,
    extraLarge = NovaDefaultShapes.extraLarge
)

val LocalNovaShapes = staticCompositionLocalOf { NovaDefaultShapes }
