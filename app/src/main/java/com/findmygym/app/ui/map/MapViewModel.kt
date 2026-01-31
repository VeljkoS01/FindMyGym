package com.findmygym.app.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmygym.app.data.gyms.GymsRepository
import com.findmygym.app.data.model.Gym
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapViewModel(
    private val repo: GymsRepository = GymsRepository()
) : ViewModel() {

    var gyms by mutableStateOf<List<Gym>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                repo.streamGyms().collectLatest { list ->
                    gyms = list
                }
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun addGym(
        name: String,
        type: String,
        desc: String,
        lat: Double,
        lng: Double,
        onDone: () -> Unit
    ) {
        error = null
        viewModelScope.launch {
            try {
                repo.addGym(name, type, desc, lat, lng)
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "Failed to add gym"
            }
        }
    }
}