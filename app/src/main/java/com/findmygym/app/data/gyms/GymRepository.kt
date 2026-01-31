package com.findmygym.app.data.gyms

import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.data.model.Gym
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.findmygym.app.data.model.GymComment


class GymsRepository(
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
                    val g = doc.toObject(Gym::class.java) ?: return@mapNotNull null
                    g.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
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
                authorUsername = me.username,
                createdAt = System.currentTimeMillis(),
                avgRating = 0.0,
                ratingCount = 0
            )

            tx.set(gymRef, gym)

            // +5 points for adding a gym
            val userRef = db.collection("users").document(uid)
            tx.update(userRef, "points", FieldValue.increment(5))

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
                authorUsername = me.username,
                text = text.trim(),
                createdAt = System.currentTimeMillis()
            )

            tx.set(commentRef, c)

            // +2 points for commenting
            val userRef = db.collection("users").document(uid)
            tx.update(userRef, "points", FieldValue.increment(2))

            null
        }.await()
    }
}