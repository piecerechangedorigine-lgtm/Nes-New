package com.novafinance.feature.assistant.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.assistant.AssistantRoute
import javax.inject.Inject

class AssistantNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.ASSISTANT

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            AssistantRoute(
                onOpenBudgets = { navController.navigate(NovaRoutes.BUDGETS) },
                onOpenGoals = { navController.navigate(NovaRoutes.GOALS) },
                onOpenAnalytics = { navController.navigate(NovaRoutes.ANALYTICS) },
                onAddTransaction = { navController.navigate(NovaRoutes.transactions()) }
            )
        }
    }
}
