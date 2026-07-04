package com.novafinance.feature.transactions.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.transactions.TransactionsRoute
import javax.inject.Inject

class TransactionsNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.TRANSACTIONS

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(
            route = NovaRoutes.TRANSACTIONS_ROUTE_PATTERN,
            arguments = listOf(
                navArgument(NovaRoutes.TRANSACTIONS_ARG_ACCOUNT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            TransactionsRoute(onBack = { navController.popBackStack() })
        }
    }
}
