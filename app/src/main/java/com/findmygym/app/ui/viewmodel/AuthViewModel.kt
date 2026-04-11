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
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                repo.login(email, password)
                onSuccess()
            } catch (e: Exception) {
                error = e.message ?: "Login failed"
            } finally {
                loading = false
            }
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String,
        onSuccess: () -> Unit
    ) {
        loading = true
        error = null
        viewModelScope.launch {
            try {
                repo.register(email, password, fullName, phone)
                onSuccess()
            } catch (e: Exception) {
                error = e.message ?: "Registration failed"
            } finally {
                loading = false
            }
        }
    }

    fun setRememberMe(store: RememberMeStore, value: Boolean) {
        viewModelScope.launch {
            store.setRememberMe(value)
        }
    }

    fun logout() {
        repo.logout()
    }

    fun showError(message: String?) {
        error = message
    }

    fun clearError() {
        error = null
    }
}