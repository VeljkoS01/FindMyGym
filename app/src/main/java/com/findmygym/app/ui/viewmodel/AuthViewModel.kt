package com.findmygym.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.auth.RememberMeStore
import kotlinx.coroutines.launch

class AuthViewModel(
    //AuthRepository za komunikaciju sa Firebase-om
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    //Prilikom login-a koristi se za prikaz loading stanja
    var loading by mutableStateOf(false)
        private set

    //Cuva poruke za error
    var error by mutableStateOf<String?>(null)
        private set


    fun login(email: String, password: String, onSuccess: () -> Unit) {
        //Kada korisnik klikne login, ukljucuje se loading i brisu se stare greske
        loading = true
        clearError()

        viewModelScope.launch {
            try {
                //Poziv Repository za login i poziva se callback za dalju navigaciju
                authRepository.login(email, password)
                onSuccess()
            } catch (e: Exception) {
                error = e.message ?: "Login failed"
            } finally {
                loading = false
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        password: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        //Prilikom register-a koristi se za prikaz loading stanja
        loading = true
        clearError()

        viewModelScope.launch {
            try {
                //Poziv Repository za register i poziva se callback za dalju navigaciju
                authRepository.register(fullName,email, password, phone)
                onSuccess()
            } catch (e: Exception) {
                error = e.message ?: "Registration failed"
            } finally {
                loading = false
            }
        }
    }

    fun setRememberMe(store: RememberMeStore, value: Boolean) {
        // Upis vrednosti "Remember me" u lokalni DataStore
        viewModelScope.launch {
            store.setRememberMe(value)
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun showError(message: String?) {
        error = message
    }

    fun clearError() {
        error = null
    }
}