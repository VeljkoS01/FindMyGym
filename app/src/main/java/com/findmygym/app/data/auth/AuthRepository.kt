package com.findmygym.app.data.auth

import com.findmygym.app.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.FieldValue


class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun currentUid(): String? = auth.currentUser?.uid
    fun logout() = auth.signOut()

    suspend fun login(emailRaw: String, password: String) {
        val email = emailRaw.trim()
        if (email.isBlank()) throw Exception("Please enter your email")
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(
        emailRaw: String,
        password: String,
        fullName: String,
        phone: String
    ) {
        val email = emailRaw.trim()

        if (email.isBlank()) throw Exception("Please enter your email")
        if (password.length < 6) throw Exception("Password must be at least 6 characters")
        if (fullName.trim().isBlank()) throw Exception("Please enter your full name")
        if (phone.trim().isBlank()) throw Exception("Please enter your phone number")

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Error: no user")
        val uid = user.uid

        val profile = AppUser(
            uid = uid,
            email = email,
            fullName = fullName.trim(),
            phone = phone.trim(),
            points = 0
        )

        db.collection("users").document(uid).set(profile).await()
    }

    suspend fun getMyProfile(): AppUser? {
        val uid = currentUid() ?: return null
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(AppUser::class.java)
    }

    suspend fun updateMyLocation(lat: Double, lng: Double) {
        val uid = currentUid() ?: return
        db.collection("users").document(uid).update(
            mapOf(
                "lastLat" to lat,
                "lastLng" to lng,
                "lastLocationAt" to System.currentTimeMillis()
            )
        ).await()
    }

}
