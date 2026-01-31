package com.findmygym.app.ui.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(onBack: () -> Unit) {
    val vm: LeaderboardViewModel = viewModel()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            vm.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                itemsIndexed(vm.users) { idx, u ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text("#${idx + 1}", modifier = Modifier.width(48.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.username)
                            if (u.fullName.isNotBlank()) {
                                Text(u.fullName, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("${u.points} pts")
                    }
                    Divider()
                }
            }
        }
    }
}