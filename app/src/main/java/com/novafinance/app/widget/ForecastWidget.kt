package com.novafinance.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.novafinance.core.domain.model.ForecastSummary
import com.novafinance.core.domain.model.NovaResult
import kotlinx.coroutines.flow.first

/** This month's spending-pace projection (Phase 8.5.6's Forecast Engine), read straight through [WidgetEntryPoint] — same 30-minute Android-enforced refresh floor as [GoalProgressWidget]. */
class ForecastWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val forecast = fetchForecast(context)

        provideContent {
            GlanceTheme {
                WidgetSurface {
                    if (forecast == null) {
                        EmptyWidgetMessage("Open Nova to see your monthly forecast.")
                    } else {
                        ForecastContent(forecast)
                    }
                }
            }
        }
    }

    /** `.first()`, not `.collect` — see [com.novafinance.app.widget.GoalProgressWidget]'s fetch function doc for why. */
    private suspend fun fetchForecast(context: Context): ForecastSummary? {
        val useCase = context.widgetEntryPoint().getDreamDashboardData()
        val result = useCase(Unit).first()
        return (result as? NovaResult.Success)?.data?.forecast
    }
}

class ForecastWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ForecastWidget()
}

@Composable
private fun ForecastContent(forecast: ForecastSummary) {
    Text(
        text = "This month's forecast",
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = ColorProvider(androidx.compose.ui.graphics.Color(0xFFB3B3BA)))
    )
    Text(
        text = forecast.message,
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(androidx.compose.ui.graphics.Color.White))
    )
}
