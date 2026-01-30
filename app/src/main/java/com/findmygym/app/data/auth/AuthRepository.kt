package com.findmygym.app.data.auth

import com.findmygym.app.data.model.AppUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    fun isLoggedIn(): Boolean = auth.currentUser != null
    fun currentUid(): String? = auth.currentUser?.uid
    fun logout() = auth.signOut()

    private fun normalizeUsername(raw: String): String {
        val u = raw.trim().lowercase()
        val cleaned = u.replace(Regex("[^a-z0-9._-]"), "")
        if (cleaned.isBlank()) throw Exception("Username can contain only: a-z 0-9 . _ -")
        return cleaned
    }

    private fun usernameToEmail(username: String): String {
        return "$username@findmygym.app"
    }

    suspend fun login(usernameRaw: String, password: String) {
        val username = normalizeUsername(usernameRaw)
        val email = usernameToEmail(username)
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun register(
        usernameRaw: String,
        password: String,
        fullName: String,
        phone: String,
        photoBase64: String?
    ) {
        val username = normalizeUsername(usernameRaw)

        if (password.length < 6) throw Exception("Password must be at least 6 characters")
        if (fullName.trim().isBlank()) throw Exception("Please enter your full name")
        if (phone.trim().isBlank()) throw Exception("Please enter your phone number")

        val email = usernameToEmail(username)

        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Error: no user")
        val uid = user.uid

        try {
            db.runTransaction { tx ->
                val usernameRef = db.collection("usernames").document(username)
                val usernameSnap = tx.get(usernameRef)

                if (usernameSnap.exists()) {
                    throw IllegalStateException("Username is already taken")
                }

                tx.set(
                    usernameRef,
                    mapOf(
                        "uid" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                val profileRef = db.collection("users").document(uid)
                val profile = AppUser(
                    uid = uid,
                    username = username,
                    fullName = fullName.trim(),
                    phone = phone.trim(),
                    photoBase64 = photoBase64,
                    points = 0
                )
                tx.set(profileRef, profile)

                null
            }.await()
        } catch (e: Exception) {
            try { user.delete().await() } catch (_: Exception) {}
            throw Exception(e.message ?: "Registration failed")
        }
    }

    suspend fun getMyProfile(): AppUser? {
        val uid = currentUid() ?: return null
        val snap = db.collection("users").document(uid).get().await()
        return snap.toObject(AppUser::class.java)
    }
}