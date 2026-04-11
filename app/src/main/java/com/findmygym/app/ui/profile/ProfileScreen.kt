package com.findmygym.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onFocusGym: (Double, Double) -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val profile = viewModel.profile
    val loading = viewModel.loading
    val error = viewModel.error
    val deleting = viewModel.deleting
    val deleteError = viewModel.deleteError
    val myGyms = viewModel.myGyms

    var showMyGyms by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReauth by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }

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
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
        }

        if (profile == null) {
            Text("No profile loaded.")
            return@Column
        }

        Text(
            text = profile.fullName.ifBlank { "User" },
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(6.dp))
        Text(profile.email)

        if (profile.phone.isNotBlank()) {
            Text(profile.phone)
        }

        Spacer(Modifier.height(12.dp))
        Text("Points: ${profile.points}")

        Spacer(Modifier.height(18.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showMyGyms = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = !deleting
        ) {
            Text("My gyms")
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                viewModel.clearDeleteError()
                showDeleteConfirm = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !deleting
        ) {
            Text(if (deleting) "Deleting..." else "Delete account")
        }

        deleteError?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    if (showMyGyms) {
        MyGymsDialog(
            gyms = myGyms,
            onDismiss = { showMyGyms = false },
            onSelect = { gym ->
                showMyGyms = false
                onFocusGym(gym.lat, gym.lng)
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (!deleting) showDeleteConfirm = false
            },
            title = {
                Text("Delete account")
            },
            text = {
                Text("This will permanently delete your account, all gyms you added and all ratings and comments. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        viewModel.clearDeleteError()
                        showDeleteConfirm = false
                        showReauth = true
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        viewModel.clearDeleteError()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReauth) {
        AlertDialog(
            onDismissRequest = {
                if (!deleting) showReauth = false
            },
            title = {
                Text("Confirm password")
            },
            text = {
                Column {
                    Text("Enter your password to delete the account.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        enabled = !deleting
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deleting && reauthPassword.isNotBlank(),
                    onClick = {
                        viewModel.deleteAccount(
                            password = reauthPassword,
                            onSuccess = {
                                reauthPassword = ""
                                showReauth = false
                                onAccountDeleted()
                            }
                        )
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deleting,
                    onClick = {
                        reauthPassword = ""
                        viewModel.clearDeleteError()
                        showReauth = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}