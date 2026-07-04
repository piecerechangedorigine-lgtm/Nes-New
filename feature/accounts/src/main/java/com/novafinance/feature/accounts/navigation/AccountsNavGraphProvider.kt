package com.novafinance.feature.accounts.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.accounts.AccountsRoute
import javax.inject.Inject

class AccountsNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.ACCOUNTS

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            AccountsRoute(
                onOpenAccountTransactions = { accountId ->
                    navController.navigate(NovaRoutes.transactions(accountId))
                }
            )
        }
    }
}
