package com.novafinance.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.novafinance.core.navigation.NovaNavGraphProvider

/**
 * Builds the full app navigation graph by delegating to each feature's
 * own [NovaNavGraphProvider]. This file never references a feature
 * composable directly — that boundary is what keeps modules independently
 * buildable and testable.
 */
@Composable
fun NovaNavHost(
    navController: NavHostController,
    navGraphProviders: Set<@JvmSuppressWildcards NovaNavGraphProvider>,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NovaDestination.Dashboard.route,
        modifier = modifier
    ) {
        navGraphProviders.forEach { provider ->
            provider.registerGraph(this, navController)
        }
    }
}
