package com.findmygym.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
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
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Search


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScaffold(
    title: String,
    currentRoute: String,
    onGoProfile: () -> Unit,
    onGoLeaderboard: () -> Unit,
    onGoGymList: () -> Unit,
    onGoAddGym: () -> Unit,
    onLogout: () -> Unit,
    onOpenFilters: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun closeDrawerThen(action: () -> Unit) {
        scope.launch {
            drawerState.close()
            action()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Text("Find My Gym", modifier = Modifier.padding(16.dp))

                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = currentRoute == "profile",
                    onClick = { closeDrawerThen(onGoProfile) }
                )
                NavigationDrawerItem(
                    label = { Text("Leaderboard") },
                    selected = currentRoute == "leaderboard",
                    onClick = { closeDrawerThen(onGoLeaderboard) }
                )
                NavigationDrawerItem(
                    label = { Text("Gym list") },
                    selected = false,
                    onClick = { closeDrawerThen(onGoGymList) }
                )
                NavigationDrawerItem(
                    label = { Text("Add gym") },
                    selected = false,
                    onClick = { closeDrawerThen(onGoAddGym) }
                )
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = { closeDrawerThen(onLogout) }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        onOpenFilters?.let { open ->
                            IconButton(onClick = open) {
                                Icon(Icons.Filled.Search, contentDescription = "Filters")
                            }
                        }
                    }
                )
            },
            content = content
        )
    }
}
