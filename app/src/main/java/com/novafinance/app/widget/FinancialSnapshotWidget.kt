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
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.novafinance.core.domain.model.BalanceOverview
import com.novafinance.core.domain.model.NovaResult
import com.novafinance.core.domain.model.formatted
import kotlinx.coroutines.flow.first

private val LabelColor = ColorProvider(Color(0xFFB3B3BA))
private val ValueColor = ColorProvider(Color.White)

/** Total Liquidity and Available Spending Power (Phase 8.5.3's Hybrid Balance Model), read straight through [WidgetEntryPoint]. */
class FinancialSnapshotWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val overview = fetchOverview(context)

        provideContent {
            GlanceTheme {
                WidgetSurface {
                    if (overview == null) {
                        EmptyWidgetMessage("Open Nova to see your financial snapshot.")
                    } else {
                        FinancialSnapshotContent(overview)
                    }
                }
            }
        }
    }

    /** `.first()`, not `.collect` — see [com.novafinance.app.widget.GoalProgressWidget]'s fetch function doc for why. */
    private suspend fun fetchOverview(context: Context): BalanceOverview? {
        val useCase = context.widgetEntryPoint().getDreamDashboardData()
        val result = useCase(Unit).first()
        return (result as? NovaResult.Success)?.data?.balanceOverview
    }
}

class FinancialSnapshotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinancialSnapshotWidget()
}

@Composable
private fun FinancialSnapshotContent(overview: BalanceOverview) {
    SnapshotRow(label = "Spending power", value = overview.availableSpendingPower.formatted())
    Spacer(modifier = GlanceModifier.height(6.dp))
    SnapshotRow(label = "Liquidity", value = overview.totalLiquidity.formatted())
}

@Composable
private fun SnapshotRow(label: String, value: String) {
    Text(text = label, style = TextStyle(fontSize = 12.sp, color = LabelColor))
    Text(text = value, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ValueColor))
}
