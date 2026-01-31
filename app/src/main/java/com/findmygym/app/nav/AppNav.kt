package com.findmygym.app.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.findmygym.app.ui.leaderboard.LeaderboardScreen
import com.findmygym.app.ui.login.LoginScreen
import com.findmygym.app.ui.login.RegisterScreen
import com.findmygym.app.ui.map.MapScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onGoRegister = { nav.navigate(Routes.REGISTER) },
                onGoMap = {
                    nav.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { nav.popBackStack() },
                onGoMap = {
                    nav.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAP) {
            MapScreen(
                onGoLeaderboard = { nav.navigate(Routes.LEADERBOARD) }
            )
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { nav.popBackStack() })
        }
    }
}