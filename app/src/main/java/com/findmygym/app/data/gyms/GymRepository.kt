package com.findmygym.app.data.gyms

import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.model.Gym
import com.findmygym.app.data.model.GymComment
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions


class GymRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepo: AuthRepository = AuthRepository()
) {

    fun streamGyms(): Flow<List<Gym>> = callbackFlow {
        val reg = db.collection("gyms")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    val gym = doc.toObject(Gym::class.java) ?: return@mapNotNull null
                    gym.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    fun streamMyGyms(): Flow<List<Gym>> {
        val uid = authRepo.currentUid()
        return streamGyms().map { list ->
            if (uid.isNullOrBlank()) emptyList()
            else list.filter { it.authorUid == uid }
                .sortedByDescending { it.createdAt }
        }
    }

    suspend fun addGym(
        name: String,
        type: String,
        description: String,
        lat: Double,
        lng: Double
    ) {
        val uid = authRepo.currentUid() ?: throw Exception("Not logged in")
        val me = authRepo.getMyProfile() ?: throw Exception("No profile")

        val gymRef = db.collection("gyms").document()

        db.runTransaction { tx ->
            val gym = Gym(
                id = gymRef.id,
                name = name.trim(),
                type = type.trim().ifBlank { "Gym" },
                description = description.trim(),
                lat = lat,
                lng = lng,
                authorUid = uid,
                authorUsername = me.fullName.ifBlank { me.email },
                createdAt = System.currentTimeMillis(),
                avgRating = 0.0,
                ratingCount = 0
            )

            tx.set(gymRef, gym)

            val userRef = db.collection("users").document(uid)
            tx.set(userRef, mapOf("points" to FieldValue.increment(5)), SetOptions.merge())


            null
        }.await()
    }

    fun streamComments(gymId: String): Flow<List<GymComment>> = callbackFlow {
        val reg = db.collection("gyms").document(gymId)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                val list = snap?.documents?.mapNotNull { doc ->
                    val c = doc.toObject(GymComment::class.java) ?: return@mapNotNull null
                    c.copy(id = doc.id)
                } ?: emptyList()

                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    suspend fun addComment(gymId: String, text: String) {
        val uid = authRepo.currentUid() ?: throw Exception("Not logged in")
        val me = authRepo.getMyProfile() ?: throw Exception("No profile")

        val commentRef = db.collection("gyms").document(gymId)
            .collection("comments").document()

        db.runTransaction { tx ->
            val c = GymComment(
                id = commentRef.id,
                gymId = gymId,
                authorUid = uid,
                authorUsername = me.fullName.ifBlank { me.email },
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )

            tx.set(commentRef, c)

            val userRef = db.collection("users").document(uid)
            tx.set(userRef, mapOf("points" to FieldValue.increment(2)), SetOptions.merge())


            null
        }.await()
    }

    suspend fun rateGym(gymId: String, value: Int) {
        require(value in 1..5) { "Rating must be 1..5" }

        val uid = authRepo.currentUid() ?: throw Exception("Not logged in")
        val me = authRepo.getMyProfile()

        val gymRef = db.collection("gyms").document(gymId)
        val ratingRef = gymRef.collection("ratings").document(uid)
        val userRef = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val ratingSnap = tx.get(ratingRef)
            val gymSnap = tx.get(gymRef)

            if (!gymSnap.exists()) {
                throw IllegalStateException("Gym not found")
            }

            if (ratingSnap.exists()) {
                throw IllegalStateException("You already rated this gym")
            }

            val oldAvg = (gymSnap.getDouble("avgRating") ?: 0.0)
            val oldCount = (gymSnap.getLong("ratingCount") ?: 0L)

            val newCount = oldCount + 1
            val newAvg = ((oldAvg * oldCount) + value) / newCount.toDouble()

            tx.set(
                ratingRef,
                mapOf(
                    "value" to value,
                    "authorUid" to uid,
                    "authorUsername" to (me?.fullName?.ifBlank { me.email } ?: ""),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            tx.update(
                gymRef,
                mapOf(
                    "avgRating" to newAvg,
                    "ratingCount" to newCount
                )
            )

            tx.set(
                userRef,
                mapOf("points" to FieldValue.increment(1)),
                SetOptions.merge()
            )

            null
        }.await()
    }


    suspend fun hasMyRating(gymId: String): Boolean {
        val uid = authRepo.currentUid() ?: return false
        val snap = db.collection("gyms").document(gymId)
            .collection("ratings").document(uid)
            .get().await()
        return snap.exists()
    }
}
