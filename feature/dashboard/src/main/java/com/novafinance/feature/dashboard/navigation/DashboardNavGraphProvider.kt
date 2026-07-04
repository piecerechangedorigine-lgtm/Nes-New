package com.novafinance.feature.dashboard.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.dashboard.DashboardRoute
import com.novafinance.feature.dashboard.studio.DashboardStudioRoute
import javax.inject.Inject

class DashboardNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.DASHBOARD

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            DashboardRoute(
                onAddTransaction = { navController.navigate(NovaRoutes.transactions()) },
                onOpenAccounts = { navController.navigate(NovaRoutes.ACCOUNTS) },
                onOpenBudgets = { navController.navigate(NovaRoutes.BUDGETS) },
                onOpenGoals = { navController.navigate(NovaRoutes.GOALS) },
                onOpenAnalytics = { navController.navigate(NovaRoutes.ANALYTICS) },
                onSeeAllTransactions = { navController.navigate(NovaRoutes.transactions()) },
                onOpenAssistant = { navController.navigate(NovaRoutes.ASSISTANT) },
                onOpenDashboardStudio = { navController.navigate(NovaRoutes.DASHBOARD_STUDIO) },
                onOpenDebt = { navController.navigate(NovaRoutes.DEBT) }
            )
        }
        builder.composable(NovaRoutes.DASHBOARD_STUDIO) {
            DashboardStudioRoute(onBack = { navController.popBackStack() })
        }
    }
}
