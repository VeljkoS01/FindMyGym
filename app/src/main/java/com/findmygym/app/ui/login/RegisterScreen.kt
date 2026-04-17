package com.findmygym.app.ui.login

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findmygym.app.ui.components.textFieldTextStyle
import com.findmygym.app.ui.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onBackToLogin: () -> Unit,
    onGoMap: () -> Unit
) {
    //ViewModel upravlja registracijom, loading stanjem i greskama
    val viewModel: AuthViewModel = viewModel()
    val focusManager = LocalFocusManager.current

    //Lokalna stanja svih input polja
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    //Prikaz lozinke i potvrde lozinke
    var passVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            //Klik van polja uklanja fokus sa inputa
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                //Omogucava scroll ako sadrzaj ne moze da stane na ekran
                .verticalScroll(rememberScrollState())
                //Dodavanje paddinga zbog tastature kada se otvori
                .imePadding(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Create account",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(16.dp))

            //Polje za unos imena
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                textStyle = textFieldTextStyle(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            //Polje za unos Email-a
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                textStyle = textFieldTextStyle(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            //Polje za unos sifre, po defaultu skriveno
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 6)") },
                textStyle = textFieldTextStyle(),
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
            Spacer(Modifier.height(10.dp))

            //Polje za unos potvrde sifre, po defaultu skriveno
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                textStyle = textFieldTextStyle(),
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (confirmVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            //Polje za unos broja telefona
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                textStyle = textFieldTextStyle(),
                modifier = Modifier.fillMaxWidth()
            )

            //Prikaz greska
            viewModel.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))

            //Dugme za register
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.clearError()

                    //Greska ukoliko se sifre ne poklapaju
                    if (password != confirmPassword) {
                        viewModel.showError("Passwords do not match")
                        return@Button
                    }

                    //Registracija
                    viewModel.register(fullName, email, password, phone) { onGoMap() }
                },
                enabled = !viewModel.loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.loading) "Creating..." else "Create account")
            }

            Spacer(Modifier.height(10.dp))

            //Povratak na Login
            OutlinedButton(
                onClick = {
                    focusManager.clearFocus()
                    onBackToLogin()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}
