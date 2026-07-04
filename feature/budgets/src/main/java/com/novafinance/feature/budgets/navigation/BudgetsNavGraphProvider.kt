package com.novafinance.feature.budgets.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.budgets.BudgetsRoute
import javax.inject.Inject

class BudgetsNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.BUDGETS

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            BudgetsRoute(onBack = { navController.popBackStack() })
        }
    }
}
