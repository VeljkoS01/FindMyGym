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
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    var users by mutableStateOf<List<AppUser>>(emptyList())
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                val snap = db.collection("users")
                    .orderBy("points", Query.Direction.DESCENDING)
                    .limit(50)
                    .get()
                    .await()

                users = snap.toObjects(AppUser::class.java)
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
}