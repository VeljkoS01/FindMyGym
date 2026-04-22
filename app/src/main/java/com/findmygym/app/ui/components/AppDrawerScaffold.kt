package com.findmygym.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.findmygym.app.nav.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScaffold(
    title: String,
    currentRoute: String,
    //Navigacija izmedjuu ekrana
    onGoMap: () -> Unit,
    onGoProfile: () -> Unit,
    onGoLeaderboard: () -> Unit,
    //Akcije koje se izvrsavaju na Map ekranu
    onGoGymList: () -> Unit,
    onGoAddGym: () -> Unit,
    //Logout
    onLogout: () -> Unit,
    //Opcionalno dugme za filtere (Vidljivo samo na mapi)
    onOpenFilters: (() -> Unit)? = null,
    //Glavni sadrzaj ekrana
    content: @Composable (PaddingValues) -> Unit
) {
    //Stanje drawer-a (otvoren/zatvoren)
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    //Coroutine scope za otvaranje/zatvaranje drawer-a bez blokiranja aplikacije
    val scope = rememberCoroutineScope()

    //Pomocna funkcija: prvo zatvori drawer pa onda izvrzi akciju
    fun closeDrawerThen(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,

        //Da bi se izbeglo otvaranje drawer-a prilikom pomeranje mape prstom
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight()
            ) {
                //Header drawer-a (naziv aplikacije + dugme za zatvaranje)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Find My Gym")
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close menu")
                    }
                }

                Spacer(Modifier.height(4.dp))

                //Navigacija ka mapi
                NavigationDrawerItem(
                    label = { Text("Map") },
                    selected = currentRoute == Routes.MAP,
                    onClick = { closeDrawerThen(onGoMap) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                //Navigacija ka Profilu
                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = currentRoute == Routes.PROFILE,
                    onClick = { closeDrawerThen(onGoProfile) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                //Navigacija ka Leaderboard-u
                NavigationDrawerItem(
                    label = { Text("Leaderboard") },
                    selected = currentRoute == Routes.LEADERBOARD,
                    onClick = { closeDrawerThen(onGoLeaderboard) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                //Akcija koja otvara gymlist
                NavigationDrawerItem(
                    label = { Text("Gym list") },
                    selected = false,
                    onClick = { closeDrawerThen(onGoGymList) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                //Akcija koja pokrece dodavanje nove teretane
                NavigationDrawerItem(
                    label = { Text("Add gym") },
                    selected = false,
                    onClick = { closeDrawerThen(onGoAddGym) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.weight(1f))

                //Logout dugme na dnu ekrana
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = { closeDrawerThen(onLogout) },
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    //Dugme za otvaranje drawer-a
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    //Opcionalno dugme za filtere koje se prikazuje na Map ekranu
                    actions = {
                        onOpenFilters?.let { open ->
                            IconButton(onClick = open) {
                                Icon(Icons.Filled.Search, contentDescription = "Filters")
                            }
                        }
                    }
                )
            },
            // Prosleđujemo padding content-u zbog TopAppBar-a
            content = content
        )
    }
}
