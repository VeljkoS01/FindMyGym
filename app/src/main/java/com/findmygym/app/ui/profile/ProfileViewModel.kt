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
    //Repository za korisnika i auth operacije
    private val authRepository: AuthRepository = AuthRepository(),
    //Repository za rad sa teretanama
    private val gymRepository: GymRepository = GymRepository()
) : ViewModel() {

    //Trenutno ucitan profil korisnika
    var profile by mutableStateOf<AppUser?>(null)
        private set

    //Stanje za loading
    var loading by mutableStateOf(true)
        private set

    //Greske
    var error by mutableStateOf<String?>(null)
        private set

    //Stanje za deleting
    var deleting by mutableStateOf(false)
        private set

    //Greska pri brisanju naloga
    var deleteError by mutableStateOf<String?>(null)
        private set

    //Lista teretana koje je korisnik dodao
    var myGyms by mutableStateOf<List<Gym>>(emptyList())
        private set

    //Odmah po kreiranju ViewModel-a ucitavam profil i pratim moje teretane
    init {
        loadProfile()
        observeMyGyms()
    }

    fun loadProfile() {
        viewModelScope.launch {
            //Pre ucitavanja ukljucujemo loading i brisemo staru gresku
            loading = true
            error = null
            try {
                //Uzima profil trenutno ulogovanog korisnika
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
            //Slusa promene mojih teretana u relnom vremenu
            gymRepository.streamMyGyms().collectLatest { gyms ->
                myGyms = gyms
            }
        }
    }

    fun deleteAccount(
        password: String,
        onSuccess: () -> Unit
    ) {
        //Pokrece stanje brisanja i brise staru gresku
        viewModelScope.launch {
            deleting = true
            deleteError = null

            try {
                //Firebase trazi ponovnu potvrdu identiteta pre brisanja naloga
                authRepository.reauthenticateWithPassword(password)
                //Brisanje auth naloga i svih povezane podataka iz baze
                authRepository.deleteAccountAndData()
                //logout
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