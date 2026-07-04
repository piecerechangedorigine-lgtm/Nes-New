package com.novafinance.app.navigation

import com.novafinance.core.navigation.NovaRoutes

/**
 * Single source of truth for every reachable screen in the app.
 *
 * Top-level destinations (bottom nav, max 4 per UX requirement):
 * Dashboard, Accounts, Analytics, Profile.
 *
 * Everything else (Transactions, Budgets, Goals, Assistant) is reached
 * via deep links from those four, keeping navigation shallow. Route
 * strings themselves live in core:navigation's NovaRoutes — this sealed
 * class exists for the bottom-bar's own presentation needs (icon, label),
 * not to redefine routing.
 */
sealed class NovaDestination(val route: String, val label: String) {

    data object Dashboard : NovaDestination(NovaRoutes.DASHBOARD, "Home")
    data object Accounts : NovaDestination(NovaRoutes.ACCOUNTS, "Accounts")
    data object Analytics : NovaDestination(NovaRoutes.ANALYTICS, "Analytics")
    data object Profile : NovaDestination(NovaRoutes.PROFILE, "Profile")

    companion object {
        /** The four primary destinations shown in bottom navigation. */
        val bottomNavDestinations = listOf(Dashboard, Accounts, Analytics, Profile)
    }
}
