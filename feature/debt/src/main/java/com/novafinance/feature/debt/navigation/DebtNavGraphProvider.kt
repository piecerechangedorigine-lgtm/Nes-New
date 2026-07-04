package com.novafinance.feature.debt.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.debt.DebtRoute
import com.novafinance.feature.debt.DebtSimulatorRoute
import javax.inject.Inject

class DebtNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.DEBT

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            DebtRoute(
                onBack = { navController.popBackStack() },
                onOpenSimulator = { navController.navigate(NovaRoutes.DEBT_SIMULATOR) }
            )
        }
        builder.composable(NovaRoutes.DEBT_SIMULATOR) {
            DebtSimulatorRoute(onBack = { navController.popBackStack() })
        }
    }
}
