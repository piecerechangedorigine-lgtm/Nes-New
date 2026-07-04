package com.novafinance.app.widget

import com.novafinance.core.domain.usecase.GetDreamDashboardDataUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

/**
 * Glance widgets are constructed by the system (via
 * `GlanceAppWidgetReceiver`), not by Hilt — there's no `@AndroidEntryPoint`
 * equivalent for `GlanceAppWidget`. [EntryPointAccessors.fromApplication]
 * is the standard, documented way to reach into the Hilt graph from a
 * class Hilt doesn't construct, which is exactly this situation.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getDreamDashboardData(): GetDreamDashboardDataUseCase
}

fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)
