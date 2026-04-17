package com.findmygym.app.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.findmygym.app.ui.components.AppDrawerScaffold
import com.findmygym.app.ui.leaderboard.LeaderboardScreen
import com.findmygym.app.ui.login.LoginScreen
import com.findmygym.app.ui.login.RegisterScreen
import com.findmygym.app.ui.map.MapScreen
import com.findmygym.app.ui.profile.ProfileScreen
import com.findmygym.app.ui.splash.SplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findmygym.app.ui.viewmodel.AuthViewModel

@Composable
fun AppNav() {
    //Glavni kontroler za navigaciju izmedju ekrana u aplikaciji
    val navController = rememberNavController()

    //Zahtevi koje saljemo Mapi
    var requestGymList by rememberSaveable { mutableStateOf(false) }
    var requestFilters by rememberSaveable { mutableStateOf(false) }
    var requestAddGym by rememberSaveable { mutableStateOf(false) }

    //Koordinate na kojoj treba fokusirati mapu
    var focusLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var focusLng by rememberSaveable { mutableStateOf<Double?>(null) }

    //Pomocna funkcija za prelazak na Map ekran
    fun goToMap() {
        navController.navigate(Routes.MAP) { launchSingleTop = true }
    }

    //Cuva koordinate izabrane teretane i prebacuje na Map ekran
    fun focusGymOnMap(lat: Double, lng: Double) {
        focusLat = lat
        focusLng = lng
        goToMap()
    }

    /*Vraca korisnika na Splash ekran i brise prethodne ekrane iz Back stack-a,
    kako korisnik ne bi mogao da se vrati nakon logout-a ili brisanja naloga */
    fun goToSplashClearBackstack() {
        navController.navigate(Routes.SPLASH) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    //NavHost definise sve rute i koji se ekran prikazuje za svaku od njih
    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                //Ako korisnik nije prijavljen ide na Login
                onGoLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                //Ako je prijavljen ide na Map
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }


        composable(Routes.LOGIN) {
            LoginScreen(
                //Prelazak sa login na register
                onGoRegister = { navController.navigate(Routes.REGISTER) },
                //Nakon logina, otvara mapu i uklanja login iz Back stack-a
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                //Prelazak sa register na login
                onBackToLogin = { navController.popBackStack() },
                //Nakon uspesne registracije, prelazi na mapuu i brise Back stack
                onGoMap = {
                    navController.navigate(Routes.MAP) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.MAP) {
            //ViewModel za autentifikaciju i logout
            val authViewModel: AuthViewModel = viewModel()

            AppDrawerScaffold(
                title = "Map",
                currentRoute = Routes.MAP,

                //Navigacija za Drawer
                onGoMap = { navController.navigate(Routes.MAP) { launchSingleTop = true } },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                //Signal mapi da otvori/izvrsi
                onGoGymList = { requestGymList = true },
                onGoAddGym = { requestAddGym = true },

                onOpenFilters = { requestFilters = true },

                //Logout i povratak na Splash
                onLogout = {
                    authViewModel.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                MapScreen(
                    modifier = Modifier.padding(inner),
                    //Zahtev da se otvori lista teretana
                    requestGymList = requestGymList,
                    onRequestGymListConsumed = { requestGymList = false },
                    //Zahtev da se otvore filteri
                    requestFilters = requestFilters,
                    onRequestFiltersConsumed = { requestFilters = false },
                    //Zahtev da se pokrene dodavanje teretane
                    requestAddGym = requestAddGym,
                    onRequestAddGymConsumed = { requestAddGym = false },

                    //Fokusiiranje mape na prosledjene koordinate
                    focusLat = focusLat,
                    focusLng = focusLng,

                    //Kada se fokusira, resetujem ih
                    onFocusConsumed = {
                        focusLat = null
                        focusLng = null
                    }
                )
            }
        }

        composable(Routes.LEADERBOARD) {
            //ViewModel za autentifikaciju i logout
            val authViewModel: AuthViewModel = viewModel()

            AppDrawerScaffold(
                title = "Leaderboard",
                currentRoute = Routes.LEADERBOARD,

                //Navigacija za Drawer
                onGoMap = { goToMap() },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                //Kada korisnik trazi listu teretane ili dodavanje, prvo se pamti zahtev i prelazak na mapu gde cec da se akcija izvrsi
                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                //Da se ne bi prikazivali filteri jer su vezani samo za mapu
                onOpenFilters = null,

                //Logout i povratak na Splash
                onLogout = {
                    authViewModel.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                LeaderboardScreen(modifier = Modifier.padding(inner))
            }
        }

        composable(Routes.PROFILE) {
            //ViewModel za autentifikaciju i logout
            val authViewModel: AuthViewModel = viewModel()

            AppDrawerScaffold(
                title = "Profile",
                currentRoute = Routes.PROFILE,

                //Navigacija za Drawer
                onGoMap = { goToMap() },
                onGoProfile = { navController.navigate(Routes.PROFILE) { launchSingleTop = true } },
                onGoLeaderboard = { navController.navigate(Routes.LEADERBOARD) { launchSingleTop = true } },

                //Kada korisnik trazi listu teretane ili dodavanje, prvo se pamti zahtev i prelazak na mapu gde cec da se akcija izvrsi
                onGoGymList = {
                    requestGymList = true
                    goToMap()
                },
                onGoAddGym = {
                    requestAddGym = true
                    goToMap()
                },

                //Da se ne bi prikazivali filteri jer su vezani samo za mapu
                onOpenFilters = null,

                //Logout i povratak na Splash
                onLogout = {
                    authViewModel.logout()
                    goToSplashClearBackstack()
                }
            ) { inner ->
                ProfileScreen(
                    modifier = Modifier.padding(inner),
                    //Kada korisnik iz profila izabere teretanu, fokusira se na mapi
                    onFocusGym = { lat, lng -> focusGymOnMap(lat, lng) },
                    //Nakon brisanja naloga korisnik se vraća na splash ekran
                    onAccountDeleted = { goToSplashClearBackstack() }
                )
            }
        }
    }
}
