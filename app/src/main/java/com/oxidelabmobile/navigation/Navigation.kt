package com.oxidelabmobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.oxidelabmobile.ui.screens.about.AboutScreen
import com.oxidelabmobile.ui.screens.empty.EmptyStateScreen
import com.oxidelabmobile.ui.screens.lab.ActiveLabScreen
import com.oxidelabmobile.ui.screens.setup.ModelSetupScreen
import com.oxidelabmobile.ui.screens.setup.ModelSetupScreenNew
import com.oxidelabmobile.ui.screens.thinking.ThinkingModeScreen

object OxideLabDestinations {
    const val EMPTY_STATE = "empty_state"
    const val MODEL_SETUP = "model_setup"
    const val ACTIVE_LAB = "active_lab"
    const val THINKING_MODE = "thinking_mode"
    const val ABOUT_SCREEN = "about_screen"
}

@Composable
fun OxideLabNavHost(
    navController: NavHostController,
    startDestination: String = OxideLabDestinations.EMPTY_STATE
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(OxideLabDestinations.EMPTY_STATE) {
            EmptyStateScreen(
                onSelectModel = {
                    // navigate from startup (not manual)
                    navController.navigate("${OxideLabDestinations.MODEL_SETUP}?manual=false")
                }
            )
        }

        composable(
            route = "${OxideLabDestinations.MODEL_SETUP}?manual={manual}",
            arguments = listOf(navArgument("manual") { type = NavType.BoolType; defaultValue = false })
        ) { backStackEntry ->
            val openedFromMenu = backStackEntry.arguments?.getBoolean("manual") ?: false
            ModelSetupScreenNew(
                onModelLoaded = {
                    navController.navigate(OxideLabDestinations.ACTIVE_LAB)
                },
                onCancel = {
                    navController.popBackStack()
                },
                openedFromMenu = openedFromMenu
            )
        }

        composable(OxideLabDestinations.ACTIVE_LAB) {
            ActiveLabScreen(
                onThinkingMode = {
                    navController.navigate(OxideLabDestinations.THINKING_MODE)
                },
                onSettings = {
                    navController.navigate(OxideLabDestinations.ABOUT_SCREEN)
                },
                onOpenModelManager = {
                    // Opened from menu -> pass manual=true
                    navController.navigate("${OxideLabDestinations.MODEL_SETUP}?manual=true")
                }
            )
        }

        composable(OxideLabDestinations.THINKING_MODE) {
            ThinkingModeScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(OxideLabDestinations.ABOUT_SCREEN) {
            AboutScreen(
                onNavigateUp = {
                    navController.popBackStack()
                }
            )
        }
    }
}
