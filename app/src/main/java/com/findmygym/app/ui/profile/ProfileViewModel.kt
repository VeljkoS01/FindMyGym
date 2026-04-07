package com.findmygym.app.ui.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.gyms.GymRepository
import com.findmygym.app.data.model.AppUser
import com.findmygym.app.data.model.Gym
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val gymRepository: GymRepository = GymRepository()
) : ViewModel() {

    var profile by mutableStateOf<AppUser?>(null)
        private set

    var loading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var deleting by mutableStateOf(false)
        private set

    var deleteError by mutableStateOf<String?>(null)
        private set

    var myGyms by mutableStateOf<List<Gym>>(emptyList())
        private set

    init {
        loadProfile()
        observeMyGyms()
    }

    fun loadProfile() {
        viewModelScope.launch {
            loading = true
            error = null
            try {
                profile = authRepository.getMyProfile()
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    private fun observeMyGyms() {
        viewModelScope.launch {
            gymRepository.streamMyGyms().collectLatest { gyms ->
                myGyms = gyms
            }
        }
    }

    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            deleting = true
            deleteError = null

            try {
                authRepository.reauthenticateWithPassword(password)
                authRepository.deleteAccountAndData()
                authRepository.logout()
                onSuccess()
            } catch (e: Exception) {
                deleteError = e.message ?: "Delete failed"
            } finally {
                deleting = false
            }
        }
    }

    fun clearDeleteError() {
        deleteError = null
    }
}