package com.novafinance.core.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

/**
 * Contract implemented by every feature module to register its own
 * navigation graph onto the app-level NavHost.
 *
 * This keeps the app module decoupled from feature internals: it only
 * needs to know "a graph provider exists", never which composables
 * a feature contains. Feature modules contribute an implementation via
 * Hilt's @IntoSet multibinding, and the app module collects the full
 * Set<NovaNavGraphProvider> to build the graph.
 */
interface NovaNavGraphProvider {

    /** Route of this feature's start destination, used for bottom-nav graph roots. */
    val startRoute: String

    /** Registers this feature's composable destinations into the shared graph. */
    fun registerGraph(builder: NavGraphBuilder, navController: NavController)
}
