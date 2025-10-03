package com.oxidelabmobile.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.oxidelabmobile.ui.screens.empty.EmptyStateScreen
import com.oxidelabmobile.ui.screens.lab.ActiveLabScreen
import com.oxidelabmobile.ui.screens.setup.ModelSetupScreen
import com.oxidelabmobile.ui.screens.thinking.ThinkingModeScreen

object OxideLabDestinations {
    const val EMPTY_STATE = "empty_state"
    const val MODEL_SETUP = "model_setup"
    const val ACTIVE_LAB = "active_lab"
    const val THINKING_MODE = "thinking_mode"
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
                    navController.navigate(OxideLabDestinations.MODEL_SETUP)
                }
            )
        }
        
        composable(OxideLabDestinations.MODEL_SETUP) {
            ModelSetupScreen(
                onModelLoaded = {
                    navController.navigate(OxideLabDestinations.ACTIVE_LAB) {
                        popUpTo(OxideLabDestinations.EMPTY_STATE) {
                            inclusive = true
                        }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(OxideLabDestinations.ACTIVE_LAB) {
            ActiveLabScreen(
                onThinkingMode = {
                    navController.navigate(OxideLabDestinations.THINKING_MODE)
                },
                onSettings = {
                    // TODO: Navigate to settings
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
    }
}
