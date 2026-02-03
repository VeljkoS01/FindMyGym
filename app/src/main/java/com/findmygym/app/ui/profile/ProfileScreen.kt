package com.findmygym.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.gyms.GymsRepository
import com.findmygym.app.data.model.AppUser

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onFocusGym: (Double, Double) -> Unit
) {
    val repo = remember { AuthRepository() }
    val gymsRepo = remember { GymsRepository() }

    var me by remember { mutableStateOf<AppUser?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showMyGyms by remember { mutableStateOf(false) }

    val myGyms by gymsRepo.streamMyGyms().collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            me = repo.getMyProfile()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator()
            return@Column
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }

        val u = me
        if (u == null) {
            Text("No profile loaded.")
            return@Column
        }

        Text(u.fullName.ifBlank { "User" }, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(u.email)
        if (u.phone.isNotBlank()) Text(u.phone)
        Spacer(Modifier.height(12.dp))
        Text("Points: ${u.points}")

        Spacer(Modifier.height(18.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showMyGyms = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("My gyms") }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {

            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Delete account (next)") }
    }

    if (showMyGyms) {
        MyGymsDialog(
            gyms = myGyms,
            onDismiss = { showMyGyms = false },
            onSelect = { g ->
                showMyGyms = false
                onFocusGym(g.lat, g.lng)
            }
        )
    }
}
