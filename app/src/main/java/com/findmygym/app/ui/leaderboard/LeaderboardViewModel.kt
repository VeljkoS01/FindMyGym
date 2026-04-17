package com.findmygym.app.ui.leaderboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmygym.app.data.model.AppUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LeaderboardViewModel(
    //Firestore za citanje korisnika iz baze
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    //Lista korisnika za prikaz na leaderboard ekranu
    var users by mutableStateOf<List<AppUser>>(emptyList())
        private set

    //greske
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                //Ucitavanje 50 korisnika sortiranih po broju poena nerastuce
                val snap = db.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                //Firestore dokument u AppUser objekat
                users = snap.toObjects(AppUser::class.java)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
}