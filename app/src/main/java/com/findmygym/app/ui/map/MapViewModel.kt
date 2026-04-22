package com.findmygym.app.ui.map

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.gyms.GymRepository
import com.findmygym.app.data.model.Gym
import com.findmygym.app.data.model.GymComment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MapViewModel(
    //Repository za teretane, komentare i ocene
    private val gymRepository: GymRepository = GymRepository(),

    //Repository za korisnika i njegovu lokaciju
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    //Sve teretane iz baze
    var gyms by mutableStateOf<List<Gym>>(emptyList())
        private set

    //Greske
    var error by mutableStateOf<String?>(null)
        private set

    //Trenutni tekstualni filter
    var query by mutableStateOf("")

    //Trenutni radius filter u kilometrima
    var radiusKm by mutableStateOf(0)

    //Komentari za trenutno izabranu teretanu
    var comments by mutableStateOf<List<GymComment>>(emptyList())
        private set

    //Da li je korisnik vec ocenio izabranu teretanu
    var hasRated by mutableStateOf<Boolean?>(null)
        private set

    //Greska vezana za komentare i rating
    var commentError by mutableStateOf<String?>(null)
        private set

    //Loading stanja za slanje ocene i komentara
    var ratingSending by mutableStateOf(false)
        private set

    var commentSending by mutableStateOf(false)
        private set

    //Pamtimo poslednju notifikovanu teretanu i vreme notifikacije da ne bi bilo spama
    private var lastNotifiedGymId: String? = null

    //Odmah po kreiranju ViewModel-a pocinjemo da slusamo gyms kolekciju
    init {
        observeGyms()
    }

    private fun observeGyms() {
        viewModelScope.launch {
            try {
                //Prati promene teretana u realnom vremenu
                gymRepository.streamGyms().collectLatest { list ->
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
                //Dodaje novu teretanu u bazu
                gymRepository.addGym(name, type, desc, lat, lng)
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "Failed to add gym"
            }
        }
    }

    fun updateMyLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            try {
                //Periodican upis poslednje lokacije korisnika u bazu
                authRepository.updateMyLocation(lat, lng)
            } catch (_: Exception) {
            }
        }
    }

    fun checkNearbyGymsAndNotify(
        myLat: Double,
        myLng: Double,
        onNotify: (String, String) -> Unit
    ) {
        //Najbliza teretana u radiusu od 200m
        val near = gyms
            .map { gym -> gym to distanceKm(myLat, myLng, gym.lat, gym.lng) }
            .filter { it.second <= 0.2 }
            .minByOrNull { it.second }
            ?.first

        //Ako trenutno nema nijedne bliske teretane, resetujemo poslednju notifikovanu
        if (near == null) {
            lastNotifiedGymId = null
            return
        }

        //Ako je ista teretana kao prosli put, ne saljemo novu notifikaciju
        if (near.id == lastNotifiedGymId) return

        lastNotifiedGymId = near.id

        onNotify(
            "Gym nearby",
            "${near.name} is close to you"
        )

        /*
        //Cooldown za notifikacije
        val now = System.currentTimeMillis()
        val cooldownOk = now - lastNotifiedAt > 180000 //3min

        if (near != null && (near.id != lastNotifiedGymId || cooldownOk)) {
            lastNotifiedGymId = near.id
            lastNotifiedAt = now

            onNotify(
                "Gym nearby",
                "${near.name} is close to you"
            )
        }
         */
    }

    fun filteredGyms(myLat: Double?, myLng: Double?): List<Gym> {
        val q = query.trim().lowercase()

        fun matches(gym: Gym): Boolean {
            if (q.isBlank()) return true
            return gym.name.lowercase().contains(q) ||
                    gym.type.lowercase().contains(q) ||
                    gym.description.lowercase().contains(q) ||
                    gym.authorUsername.lowercase().contains(q)
        }

        fun withinRadius(gym: Gym): Boolean {
            if (radiusKm <= 0) return true
            if (myLat == null || myLng == null) return false
            val distance = distanceKm(myLat, myLng, gym.lat, gym.lng)
            return distance <= radiusKm.toDouble()
        }

        //Primena tekstualnog i radius filtera nad listom teretana
        return gyms.filter { matches(it) }.filter { withinRadius(it) }
    }

    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        //Haverisinova formula za racunanje udaljenosti izmedju dve geografske tacke
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun loadGymDetails(gymId: String) {
        //Reset stanja pri otvaranju detalja nove teretane
        hasRated = null
        commentError = null

        viewModelScope.launch {
            try {
                //Pratimo komentare za izabranu teretanu u realnom vremenu
                gymRepository.streamComments(gymId).collectLatest { list ->
                    comments = list
                }
            } catch (e: Exception) {
                commentError = e.message
            }
        }

        viewModelScope.launch {
            try {
                //Proveravamo da li je korisnik vec ostavio ocenu
                hasRated = gymRepository.hasMyRating(gymId)
            } catch (_: Exception) {
                hasRated = false
            }
        }
    }

    fun rateGym(gymId: String, value: Int) {
        ratingSending = true
        commentError = null

        viewModelScope.launch {
            try {
                //Upis ocene za izabranu teretanu
                gymRepository.rateGym(gymId, value)
                hasRated = true
            } catch (e: Exception) {
                commentError = e.message ?: "Failed to rate"
            } finally {
                ratingSending = false
            }
        }
    }

    fun addComment(gymId: String, text: String, onDone: () -> Unit) {
        commentSending = true
        commentError = null

        viewModelScope.launch {
            try {
                //Dodavanje komentara za izabranu teretanu
                gymRepository.addComment(gymId, text)
                onDone()
            } catch (e: Exception) {
                commentError = e.message ?: "Failed to comment"
            } finally {
                commentSending = false
            }
        }
    }

}