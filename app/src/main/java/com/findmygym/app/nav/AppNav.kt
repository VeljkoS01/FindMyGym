package com.findmygym.app.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.findmygym.app.ui.login.LoginScreen
import com.findmygym.app.ui.login.RegisterScreen
import com.findmygym.app.ui.map.MapScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()

    NavHost(
        navController = nav,
        startDestination = Routes.MAP // za sada MAP; kasnije kad ubacimo Firebase bice LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onGoRegister = { nav.navigate(Routes.REGISTER) },
                onGoMap = { nav.navigate(Routes.MAP) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onBackToLogin = { nav.popBackStack() }
            )
        }
        composable(Routes.MAP) {
            MapScreen()
        }
    }
}