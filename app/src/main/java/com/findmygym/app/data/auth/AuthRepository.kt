package com.findmygym.app.data.auth

import com.findmygym.app.data.model.AppUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

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
        ensureUserDocExists()
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

        val profile = AppUser(
            uid = user.uid,
            email = email,
            fullName = fullName.trim(),
            phone = phone.trim(),
            points = 0
        )

        db.collection("users").document(user.uid).set(profile).await()
    }

    suspend fun getMyProfile(): AppUser? {
        val uid = currentUid() ?: return null

        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        if (!snap.exists()) {
            val email = auth.currentUser?.email.orEmpty()
            val repaired = AppUser(
                uid = uid,
                email = email,
                fullName = "",
                phone = "",
                points = 0
            )
            ref.set(repaired, SetOptions.merge()).await()
            return repaired
        }

        return snap.toObject(AppUser::class.java)
    }

    suspend fun updateMyLocation(lat: Double, lng: Double) {
        val uid = currentUid() ?: return

        db.collection("users").document(uid).set(
            mapOf(
                "lastLat" to lat,
                "lastLng" to lng,
                "lastLocationAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    suspend fun reauthenticateWithPassword(password: String) {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val email = user.email ?: throw Exception("No email for current user")

        val cred = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(cred).await()
    }

    private fun asInt(value: Any?): Int = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        is Float -> value.toInt()
        is Number -> value.toInt()
        else -> 0
    }

    private suspend fun deleteCollectionInBatches(col: CollectionReference, batchSize: Int = 450) {
        while (true) {
            val snap = col.limit(batchSize.toLong()).get(Source.SERVER).await()
            if (snap.isEmpty) break

            val batch = db.batch()
            for (d in snap.documents) batch.delete(d.reference)
            batch.commit().await()
        }
    }

    /* Brise korisnika kao i sve teretane koje je on napravio, ocene i komentare na toj/tim teretani
    i sve ocene i komentare koje je korisnik ostavio na drugim teretanama */
    suspend fun deleteAccountAndData() {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val uid = user.uid

        // 0) Moji gyms
        val myGymsSnap = db.collection("gyms")
            .whereEqualTo("authorUid", uid)
            .get(Source.SERVER).await()

        val myGymRefs = myGymsSnap.documents.map { it.reference }
        val myGymIds = myGymsSnap.documents.map { it.id }.toHashSet()

        // Sve gyms (treba nam da ocistimo moje comments/ratings svuda)
        val allGymsSnap = db.collection("gyms").get(Source.SERVER).await()

        // 1) Obrisi moje ratings na tudjim gyms + preracunaj prosek
        try {
            for (gymDoc in allGymsSnap.documents) {
                val gymId = gymDoc.id
                val gymRef = gymDoc.reference

                // moje gyms brisemo kasnije cele
                if (myGymIds.contains(gymId)) continue

                // ratingId == uid (po tvojim pravilima)
                val ratingRef = gymRef.collection("ratings").document(uid)
                val ratingSnap = ratingRef.get(Source.SERVER).await()
                if (!ratingSnap.exists()) continue

                db.runTransaction { tx ->
                    val gSnap = tx.get(gymRef)
                    val rSnap = tx.get(ratingRef)

                    if (!gSnap.exists() || !rSnap.exists()) return@runTransaction null

                    val value = asInt(rSnap.get("value"))
                    val oldAvg = gSnap.getDouble("avgRating") ?: 0.0
                    val oldCount = gSnap.getLong("ratingCount") ?: 0L

                    val newCount = (oldCount - 1L).coerceAtLeast(0L)
                    val newAvg =
                        if (newCount == 0L) 0.0
                        else ((oldAvg * oldCount.toDouble()) - value.toDouble()) / newCount.toDouble()

                    tx.delete(ratingRef)
                    tx.update(
                        gymRef,
                        mapOf(
                            "avgRating" to newAvg,
                            "ratingCount" to newCount
                        )
                    )
                    null
                }.await()
            }
        } catch (e: Exception) {
            throw Exception("Delete failed at: my ratings. ${e.message}")
        }

        // 2) Obrisi moje comments na svim gyms
        try {
            for (gymDoc in allGymsSnap.documents) {
                val gymRef = gymDoc.reference
                val commentsCol = gymRef.collection("comments")

                while (true) {
                    val snap = commentsCol
                        .whereEqualTo("authorUid", uid)
                        .limit(450)
                        .get(Source.SERVER).await()

                    if (snap.isEmpty) break

                    val batch = db.batch()
                    for (d in snap.documents) batch.delete(d.reference)
                    batch.commit().await()
                }
            }
        } catch (e: Exception) {
            throw Exception("Delete failed at: my comments. ${e.message}")
        }

        // 3) Obrisi moje gyms + njihove subkolekcije, pa gym doc
        try {
            for (gymRef in myGymRefs) {
                deleteCollectionInBatches(gymRef.collection("ratings"))
                deleteCollectionInBatches(gymRef.collection("comments"))
                gymRef.delete().await()
            }
        } catch (e: Exception) {
            throw Exception("Delete failed at: my gyms cleanup. ${e.message}")
        }

        // 4) users/{uid}
        try {
            db.collection("users").document(uid).delete().await()
        } catch (e: Exception) {
            throw Exception("Delete failed at: user doc. ${e.message}")
        }

        // 5) Auth user
        try {
            user.delete().await()
        } catch (e: Exception) {
            throw Exception("Delete failed at: auth user. ${e.message}")
        }
    }

    private suspend fun ensureUserDocExists() {
        val uid = currentUid() ?: return

        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()
        if (snap.exists()) return

        val email = auth.currentUser?.email.orEmpty()
        val repaired = AppUser(
            uid = uid,
            email = email,
            fullName = "",
            phone = "",
            points = 0
        )
        ref.set(repaired, SetOptions.merge()).await()
    }
}
