package com.novafinance.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.novafinance.core.domain.model.GoalForecast
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.formatted
import com.novafinance.core.domain.model.health
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.first

private val WidgetSurfaceColor = Color(0xFF16161C)
private val WidgetTextPrimary = ColorProvider(Color.White)
private val WidgetTextSecondary = ColorProvider(Color(0xFFB3B3BA))

/**
 * Shows the single most urgent goal (lowest Goal Health score — same
 * selection rule `DashboardLayoutDefaults` uses when a preset caps how
 * many goal widgets show) since a home screen widget has no room for a
 * list. Real Room/DataStore data via [WidgetEntryPoint], not a
 * hardcoded placeholder.
 *
 * Refreshes on Android's standard `updatePeriodMillis` schedule (see
 * `res/xml/goal_progress_widget_info.xml`) — Android enforces a
 * 30-minute minimum for that, so this isn't a live/real-time surface,
 * the same honest constraint any Android home screen widget lives under.
 */
class GoalProgressWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val mostUrgentGoal = fetchMostUrgentGoal(context)

        provideContent {
            GlanceTheme {
                WidgetSurface {
                    if (mostUrgentGoal == null) {
                        EmptyWidgetMessage("Add a goal in Nova to see it here.")
                    } else {
                        GoalProgressContent(mostUrgentGoal)
                    }
                }
            }
        }
    }

    /**
     * `.first()`, not `.collect` — [GetDreamDashboardDataUseCase]'s
     * underlying Room/DataStore flows never complete on their own, so
     * collecting indefinitely here would leave `provideGlance` suspended
     * forever and the widget stuck on Glance's default loading state.
     * A single snapshot is also the right semantic anyway: this widget
     * only refreshes on Android's `updatePeriodMillis` schedule, not
     * live, so there's nothing to gain from staying subscribed.
     */
    private suspend fun fetchMostUrgentGoal(context: Context): GoalForecast? {
        val useCase = context.widgetEntryPoint().getDreamDashboardData()
        val result = useCase(Unit).first()
        val data = (result as? NovaResult.Success)?.data ?: return null
        return data.goalForecasts.minByOrNull { it.health().score }
    }
}

class GoalProgressWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoalProgressWidget()
}

@Composable
private fun GoalProgressContent(forecast: GoalForecast) {
    val goal = forecast.goal
    Text(
        text = goal.name,
        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = WidgetTextPrimary)
    )
    Text(
        text = "${(goal.percentComplete * 100).roundToInt()}%",
        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = WidgetTextPrimary)
    )
    Text(
        text = "${goal.remaining.formatted()} to go",
        style = TextStyle(fontSize = 12.sp, color = WidgetTextSecondary)
    )
}

/** Shared background/padding shell every Phase 9.9 widget uses, so all three read as one family on the home screen. */
@Composable
fun WidgetSurface(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetSurfaceColor))
            .padding(12.dp),
        horizontalAlignment = Alignment.Horizontal.Start,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        content()
    }
}

@Composable
fun EmptyWidgetMessage(message: String) {
    Text(
        text = message,
        style = TextStyle(fontSize = 13.sp, color = WidgetTextSecondary)
    )
}
