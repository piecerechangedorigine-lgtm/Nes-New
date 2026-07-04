package com.novafinance.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Nova's icon set is drawn in-house rather than pulled from Material Icons
 * — every glyph is built from straight lines only (no arcs/curves), the
 * same monoline construction the logomark uses, so nav icons, action
 * icons, and the brand mark read as one visual system rather than a
 * custom logo bolted onto stock iconography.
 *
 * Baked stroke color is black; every call site should apply `tint=` on
 * the `Icon()` composable (same convention as Icons.Filled.*) rather than
 * relying on this baked-in color.
 */
object NovaIcons {

    private fun icon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        ).build()

    val Home: ImageVector by lazy {
        icon("Home") {
            moveTo(4f, 20f); lineTo(4f, 10f); lineTo(12f, 3f); lineTo(20f, 10f); lineTo(20f, 20f); lineTo(4f, 20f)
            moveTo(10f, 20f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 20f)
        }
    }

    val Wallet: ImageVector by lazy {
        icon("Wallet") {
            moveTo(3f, 7f); lineTo(21f, 7f); lineTo(21f, 18f); lineTo(3f, 18f); lineTo(3f, 7f)
            moveTo(3f, 11f); lineTo(21f, 11f)
            moveTo(16f, 13f); lineTo(19f, 13f); lineTo(19f, 16f); lineTo(16f, 16f); lineTo(16f, 13f)
        }
    }

    val ChartBars: ImageVector by lazy {
        icon("ChartBars") {
            moveTo(3f, 20f); lineTo(21f, 20f)
            moveTo(6f, 20f); lineTo(6f, 13f)
            moveTo(12.5f, 20f); lineTo(12.5f, 7f)
            moveTo(19f, 20f); lineTo(19f, 11f)
        }
    }

    val Person: ImageVector by lazy {
        icon("Person") {
            moveTo(12f, 4f); lineTo(15.2f, 7.2f); lineTo(12f, 10.4f); lineTo(8.8f, 7.2f); lineTo(12f, 4f)
            moveTo(5f, 20f); lineTo(7f, 13f); lineTo(17f, 13f); lineTo(19f, 20f)
        }
    }

    val Plus: ImageVector by lazy {
        icon("Plus") {
            moveTo(12f, 5f); lineTo(12f, 19f)
            moveTo(5f, 12f); lineTo(19f, 12f)
        }
    }

    val Target: ImageVector by lazy {
        icon("Target") {
            moveTo(4f, 4f); lineTo(20f, 4f); lineTo(20f, 20f); lineTo(4f, 20f); lineTo(4f, 4f)
            moveTo(8f, 8f); lineTo(16f, 8f); lineTo(16f, 16f); lineTo(8f, 16f); lineTo(8f, 8f)
            moveTo(11f, 11f); lineTo(13f, 11f); lineTo(13f, 13f); lineTo(11f, 13f); lineTo(11f, 11f)
        }
    }

    val ArrowUpRight: ImageVector by lazy {
        icon("ArrowUpRight") {
            moveTo(7f, 17f); lineTo(17f, 7f)
            moveTo(9f, 7f); lineTo(17f, 7f); lineTo(17f, 15f)
        }
    }

    val ArrowDownRight: ImageVector by lazy {
        icon("ArrowDownRight") {
            moveTo(7f, 7f); lineTo(17f, 17f)
            moveTo(17f, 9f); lineTo(17f, 17f); lineTo(9f, 17f)
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            moveTo(16f, 10f)
            arcTo(6f, 6f, 0f, true, true, 4f, 10f)
            arcTo(6f, 6f, 0f, true, true, 16f, 10f)
            moveTo(14.5f, 14.5f); lineTo(20f, 20f)
        }
    }

    val Close: ImageVector by lazy {
        icon("Close") {
            moveTo(6f, 6f); lineTo(18f, 18f)
            moveTo(18f, 6f); lineTo(6f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") {
            moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f)
        }
    }

    val Check: ImageVector by lazy {
        icon("Check") {
            moveTo(5f, 13f); lineTo(10f, 18f); lineTo(19f, 6f)
        }
    }

    val Filter: ImageVector by lazy {
        icon("Filter") {
            moveTo(4f, 6f); lineTo(20f, 6f)
            moveTo(7f, 12f); lineTo(17f, 12f)
            moveTo(10f, 18f); lineTo(14f, 18f)
        }
    }

    /** Assistant / AI-insight glyph — a four-point star, drawn as one closed zigzag to stay monoline. */
    val Sparkle: ImageVector by lazy {
        icon("Sparkle") {
            moveTo(12f, 3f); lineTo(14f, 10f); lineTo(21f, 12f); lineTo(14f, 14f)
            lineTo(12f, 21f); lineTo(10f, 14f); lineTo(3f, 12f); lineTo(10f, 10f); lineTo(12f, 3f)
        }
    }

    val Send: ImageVector by lazy {
        icon("Send") {
            moveTo(3f, 20f); lineTo(21f, 12f); lineTo(3f, 4f); lineTo(8f, 12f); lineTo(3f, 20f)
            moveTo(8f, 12f); lineTo(21f, 12f)
        }
    }

    val Edit: ImageVector by lazy {
        icon("Edit") {
            moveTo(4f, 20f); lineTo(4f, 16f); lineTo(15f, 5f); lineTo(19f, 9f); lineTo(8f, 20f); lineTo(4f, 20f)
            moveTo(13f, 7f); lineTo(17f, 11f)
        }
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            // Disc approximated as a diamond, rays as short radiating
            // lines — straight-line-only construction, same as every
            // other glyph in this set (see the class doc comment).
            moveTo(12f, 8f); lineTo(16f, 12f); lineTo(12f, 16f); lineTo(8f, 12f); close()
            moveTo(12f, 4f); lineTo(12f, 6f)
            moveTo(12f, 18f); lineTo(12f, 20f)
            moveTo(4f, 12f); lineTo(6f, 12f)
            moveTo(18f, 12f); lineTo(20f, 12f)
            moveTo(6.4f, 6.4f); lineTo(7.8f, 7.8f)
            moveTo(16.2f, 16.2f); lineTo(17.6f, 17.6f)
            moveTo(6.4f, 17.6f); lineTo(7.8f, 16.2f)
            moveTo(16.2f, 7.8f); lineTo(17.6f, 6.4f)
        }
    }

    val Cloud: ImageVector by lazy {
        icon("Cloud") {
            // Organic outline approximated as a straight-edged polygon,
            // the same "zigzag stands in for a curve" approach Sparkle
            // already uses elsewhere in this set.
            moveTo(6f, 18f); lineTo(18f, 18f); lineTo(19.5f, 16f); lineTo(18f, 13f)
            lineTo(16f, 12.5f); lineTo(15.5f, 10f); lineTo(12.5f, 9f); lineTo(10f, 10.5f)
            lineTo(8f, 10f); lineTo(6f, 12f); lineTo(4.5f, 15f); lineTo(6f, 18f)
            close()
        }
    }

    val Storm: ImageVector by lazy {
        icon("Storm") {
            moveTo(6f, 15f); lineTo(16f, 15f); lineTo(17.5f, 13f); lineTo(16f, 10f)
            lineTo(14f, 9.5f); lineTo(13.5f, 7f); lineTo(10.5f, 6f); lineTo(8f, 7.5f)
            lineTo(6f, 7f); lineTo(4f, 9f); lineTo(3.5f, 12f); lineTo(6f, 15f)
            close()
            moveTo(13f, 17f); lineTo(10f, 22f)
            moveTo(9f, 17f); lineTo(7f, 21f)
        }
    }

    /** A small sun peeking from behind the same cloud outline [Cloud] uses — [DebtWeatherState.PARTLY_CLOUDY]'s one step sunnier than [Cloud] alone. */
    val PartlyCloudy: ImageVector by lazy {
        icon("PartlyCloudy") {
            moveTo(15f, 6f); lineTo(17f, 8f); lineTo(15f, 10f); lineTo(13f, 8f); close()
            moveTo(15f, 3.5f); lineTo(15f, 5f)
            moveTo(18.5f, 8f); lineTo(20f, 8f)
            moveTo(11.5f, 8f); lineTo(10.3f, 6.8f)
            moveTo(6f, 18f); lineTo(18f, 18f); lineTo(19.5f, 16f); lineTo(18f, 13f)
            lineTo(16f, 12.5f); lineTo(15.5f, 10.5f); lineTo(12.5f, 9.5f); lineTo(10f, 11f)
            lineTo(8f, 10.5f); lineTo(6f, 12.5f); lineTo(4.5f, 15.5f); lineTo(6f, 18f)
            close()
        }
    }

    /** [Cloud]'s outline with falling rain lines beneath it — [DebtWeatherState.RAINY], one step worse than [Cloud] alone, one step better than [Storm]. */
    val Rain: ImageVector by lazy {
        icon("Rain") {
            moveTo(6f, 15f); lineTo(18f, 15f); lineTo(19.5f, 13f); lineTo(18f, 10f)
            lineTo(16f, 9.5f); lineTo(15.5f, 7f); lineTo(12.5f, 6f); lineTo(10f, 7.5f)
            lineTo(8f, 7f); lineTo(6f, 9f); lineTo(4.5f, 12f); lineTo(6f, 15f)
            close()
            moveTo(9f, 18f); lineTo(8f, 21f)
            moveTo(13f, 18f); lineTo(12f, 21f)
            moveTo(17f, 18f); lineTo(16f, 21f)
        }
    }

    val ChevronUp: ImageVector by lazy {
        icon("ChevronUp") {
            moveTo(6f, 15f); lineTo(12f, 9f); lineTo(18f, 15f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        icon("ChevronDown") {
            moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f)
        }
    }

    val Eye: ImageVector by lazy {
        icon("Eye") {
            moveTo(2f, 12f); lineTo(6f, 7f); lineTo(18f, 7f); lineTo(22f, 12f)
            lineTo(18f, 17f); lineTo(6f, 17f); lineTo(2f, 12f)
            close()
            moveTo(14.5f, 12f); lineTo(13.5f, 13.5f); lineTo(10.5f, 13.5f); lineTo(9.5f, 12f)
            lineTo(10.5f, 10.5f); lineTo(13.5f, 10.5f); lineTo(14.5f, 12f)
            close()
        }
    }

    val EyeOff: ImageVector by lazy {
        icon("EyeOff") {
            moveTo(3f, 3f); lineTo(21f, 21f)
            moveTo(9.5f, 7.3f); lineTo(18f, 7f); lineTo(22f, 12f); lineTo(19.2f, 15.2f)
            moveTo(6.4f, 8.8f); lineTo(2f, 12f); lineTo(6f, 17f); lineTo(14.7f, 17f)
        }
    }
}
