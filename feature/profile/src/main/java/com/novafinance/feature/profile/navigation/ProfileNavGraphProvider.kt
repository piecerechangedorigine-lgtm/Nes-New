package com.novafinance.feature.profile.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.novafinance.core.navigation.NovaNavGraphProvider
import com.novafinance.core.navigation.NovaRoutes
import com.novafinance.feature.profile.PermissionCenterRoute
import com.novafinance.feature.profile.ProfileRoute
import javax.inject.Inject

class ProfileNavGraphProvider @Inject constructor() : NovaNavGraphProvider {

    override val startRoute: String = NovaRoutes.PROFILE

    override fun registerGraph(builder: NavGraphBuilder, navController: NavController) {
        builder.composable(startRoute) {
            ProfileRoute(
                onOpenPermissions = { navController.navigate(NovaRoutes.PERMISSIONS) }
            )
        }
        builder.composable(NovaRoutes.PERMISSIONS) {
            PermissionCenterRoute(onBack = { navController.popBackStack() })
        }
    }
}
