package com.findmygym.app.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.gyms.GymsRepository
import com.findmygym.app.data.model.AppUser
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onFocusGym: (Double, Double) -> Unit,
    onAccountDeleted: () -> Unit
) {
    val repo = remember { AuthRepository() }
    val gymsRepo = remember { GymsRepository() }
    val scope = rememberCoroutineScope()

    var me by remember { mutableStateOf<AppUser?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val myGyms by gymsRepo.streamMyGyms().collectAsState(initial = emptyList())
    var showMyGyms by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReauth by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }

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
            modifier = Modifier.fillMaxWidth(),
            enabled = !deleting
        ) { Text("My gyms") }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !deleting
        ) { Text(if (deleting) "Deleting..." else "Delete account") }

        deleteError?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showDeleteConfirm = false },
            title = { Text("Delete account") },
            text = { Text("This will permanently delete your account and all gyms you added. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        showDeleteConfirm = false
                        showReauth = true
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = { showDeleteConfirm = false }
                ) { Text("Cancel") }
            }
        )
    }

    if (showReauth) {
        AlertDialog(
            onDismissRequest = { if (!deleting) showReauth = false },
            title = { Text("Confirm password") },
            text = {
                Column {
                    Text("For security, enter your password to delete the account.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting && reauthPassword.isNotBlank(),
                    onClick = {
                        deleting = true
                        deleteError = null

                        scope.launch {
                            try {
                                repo.reauthenticateWithPassword(reauthPassword)
                                reauthPassword = ""

                                repo.deleteAccountAndData()
                                repo.logout()

                                showReauth = false
                                onAccountDeleted()
                            } catch (e: Exception) {
                                deleteError = e.message ?: "Delete failed"
                            } finally {
                                deleting = false
                            }
                        }
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        reauthPassword = ""
                        showReauth = false
                    }
                ) { Text("Cancel") }
            }
        )
    }
}
