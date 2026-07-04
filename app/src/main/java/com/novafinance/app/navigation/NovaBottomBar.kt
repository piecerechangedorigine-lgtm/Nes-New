package com.novafinance.app.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.novafinance.core.designsystem.Nova
import com.novafinance.core.designsystem.icons.NovaIcons

/**

* Bottom navigation surfacing the 4 primary destinations per the UX

* requirement (max 4 top-level destinations, everything else reached

* via deep links from these).
  */
  @Composable
  fun NovaBottomBar(navController: NavHostController) {
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentDestination = navBackStackEntry?.destination
  
  NavigationBar(
  containerColor = Nova.colors.surface,
  contentColor = Nova.colors.textSecondary
  ) {
  NovaDestination.bottomNavDestinations.forEach { destination ->
  val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
  
       NavigationBarItem(
         selected = selected,
         onClick = {
             navController.navigate(destination.route) {
                 popUpTo(navController.graph.findStartDestination().id) {
                     saveState = true
                 }
                 launchSingleTop = true
                 restoreState = true
             }
         },
         icon = {
             Icon(
                 imageVector = destination.icon(),
                 contentDescription = destination.label
             )
         },
         label = { Text(destination.label) },
         colors = NavigationBarItemDefaults.colors(
             selectedIconColor = Nova.colors.primary,
             selectedTextColor = Nova.colors.primary,
             unselectedIconColor = Nova.colors.textSecondary,
             unselectedTextColor = Nova.colors.textSecondary,
             indicatorColor = Nova.colors.elevatedSurfaceHigh
         )
     )
 }
  
  }
  }

private fun NovaDestination.icon(): ImageVector = when (this) {
NovaDestination.Dashboard -> NovaIcons.Home
NovaDestination.Accounts -> NovaIcons.Wallet
NovaDestination.Analytics -> NovaIcons.ChartBars
NovaDestination.Profile -> NovaIcons.Person
}
