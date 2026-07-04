package com.novafinance.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.novafinance.app.navigation.NovaBottomBar
import com.novafinance.app.navigation.NovaDestination
import com.novafinance.app.navigation.NovaNavHost
import com.novafinance.core.designsystem.NovaTheme
import com.novafinance.core.navigation.NovaNavGraphProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**

* Nova is a single-activity app. All screens are Compose destinations

* registered through each feature module's NovaNavGraphProvider — this

* class owns zero feature logic, only the app shell (theme + scaffold + nav).
  */
  @AndroidEntryPoint
  class MainActivity : ComponentActivity() {
  
  @Inject
  lateinit var navGraphProviders: Set<@JvmSuppressWildcards NovaNavGraphProvider>
  
  override fun onCreate(savedInstanceState: Bundle?) {
  installSplashScreen()
  super.onCreate(savedInstanceState)
  enableEdgeToEdge()
  
   setContent {
     NovaApp(navGraphProviders = navGraphProviders)
 }
  
  }
  }

@Composable
private fun NovaApp(
navGraphProviders: Set<@JvmSuppressWildcards NovaNavGraphProvider>
) {
NovaTheme {
val navController = rememberNavController()
val navBackStackEntry by navController.currentBackStackEntryAsState()
val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar =
        NovaDestination.bottomNavDestinations.any {
            it.route == currentRoute
        }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NovaBottomBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NovaNavHost(
            navController = navController,
            navGraphProviders = navGraphProviders,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

}
