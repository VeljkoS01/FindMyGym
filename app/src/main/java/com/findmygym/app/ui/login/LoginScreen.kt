package com.findmygym.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onGoMap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Find My Gym")
        Spacer(Modifier.height(16.dp))

        Button(onClick = onGoMap, modifier = Modifier.fillMaxWidth()) {
            Text("Nastavi (privremeno bez login-a)")
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Napravi nalog")
        }
    }
}