package com.findmygym.app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findmygym.app.data.auth.RememberMeStore
import com.findmygym.app.ui.components.fmgTextFieldTextStyle


@Composable
fun LoginScreen(
    onGoRegister: () -> Unit,
    onGoMap: () -> Unit
) {
    val vm: AuthViewModel = viewModel()
    val context = LocalContext.current
    val rememberStore = remember { RememberMeStore(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        rememberStore.rememberMeFlow.collect { v ->
            rememberMe = v
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Find My Gym",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                textStyle = fmgTextFieldTextStyle(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                textStyle = fmgTextFieldTextStyle(),
                visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passVisible = !passVisible }) {
                        Icon(
                            imageVector = if (passVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (passVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = {
                        rememberMe = it
                        vm.setRememberMe(rememberStore, it)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Remember me",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            vm.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.login(email, password, onGoMap) },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (vm.loading) "Signing in..." else "Sign in")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onGoRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create account")
            }
        }
    }
}
