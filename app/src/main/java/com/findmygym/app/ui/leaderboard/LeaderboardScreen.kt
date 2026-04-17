package com.findmygym.app.ui.leaderboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LeaderboardScreen(
    modifier: Modifier = Modifier
) {
    //ViewModel cuva listu korisnika i gresku ukoliko postoji
    val viewModel: LeaderboardViewModel = viewModel()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        viewModel.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))

        //Redni broj korisnika na leaderboard listi, fullName ili Email ukoliko ne postoji i ukupan broj poena
        LazyColumn {
            itemsIndexed(viewModel.users) { idx, u ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                    Text("${idx + 1}.", modifier = Modifier.width(48.dp))
                    Column(Modifier.weight(1f)) {
                        Text(u.fullName.ifBlank { u.email })
                    }
                    Text("${u.points} pts")
                }
                HorizontalDivider()
            }
        }
    }
}
