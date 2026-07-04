package com.novafinance.feature.analytics.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.analytics.AnalyticsRoute
import javax.inject.Inject

class AnalyticsNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.ANALYTICS

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            AnalyticsRoute(onOpenAssistant = { navController.navigate(NovaRoutes.ASSISTANT) })
        }
    }
}
