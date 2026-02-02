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

    var query by mutableStateOf("")
    var radiusKm by mutableStateOf(0) // 0 = no radius filter

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

    fun filteredGyms(myLat: Double?, myLng: Double?): List<Gym> {
        val q = query.trim().lowercase()

        fun matches(g: Gym): Boolean {
            if (q.isBlank()) return true
            return g.name.lowercase().contains(q) ||
                    g.type.lowercase().contains(q) ||
                    g.description.lowercase().contains(q) ||
                    g.authorUsername.lowercase().contains(q)
        }

        fun withinRadius(g: Gym): Boolean {
            if (radiusKm <= 0) return true
            if (myLat == null || myLng == null) return false
            val dKm = distanceKm(myLat, myLng, g.lat, g.lng)
            return dKm <= radiusKm.toDouble()
        }

        return gyms
            .filter { matches(it) }
            .filter { withinRadius(it) }
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}