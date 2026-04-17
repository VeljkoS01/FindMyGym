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
    //FirebaseAuth sluzi za rad sa teretanama, komentarima i ocenama
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    // Firestore da znamo koji je korisnik prijavljen
    private val authRepo: AuthRepository = AuthRepository()
) {

    fun streamGyms(): Flow<List<Gym>> = callbackFlow {
        //Slusa promene u "gyms" kolekciji u realnom vremenu
        val reg = db.collection("gyms")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                // Svaki dokument pretvaram u Gym objekat i postavljam id
                val list = snap?.documents?.mapNotNull { doc ->
                    val gym = doc.toObject(Gym::class.java) ?: return@mapNotNull null
                    gym.copy(id = doc.id)
                } ?: emptyList()
                trySend(list)
            }

        awaitClose { reg.remove() }
    }

    //Vraca samo teretane korisnika
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

        //Kreiramo novi dokument sa automatski generisanim id-em
        val gymRef = db.collection("gyms").document()

        //Formiranje objekta nove teretane
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

            //Upis teretane u bazu
            tx.set(gymRef, gym)

            //Dodavanje poena korisniku za kreiranje teretane
            val userRef = db.collection("users").document(uid)
            tx.set(userRef, mapOf("points" to FieldValue.increment(5)), SetOptions.merge())


            null
        }.await()
    }

    fun streamComments(gymId: String): Flow<List<GymComment>> = callbackFlow {
        //Slusa orimene komentara za konkretnu teretanu
        val reg = db.collection("gyms").document(gymId)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                //Svaki comment dokument pretvaramo u GymComment objekat
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

        //Kreiramo novi dokument komentara u comments subkolekci
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

            //Upis komentara
            tx.set(commentRef, c)

            //Dodavanje poena korisniku za ostavljen komentar
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

            //Jedan korisnik moze da oceni jednu teretanu samo jednom
            if (ratingSnap.exists()) {
                throw IllegalStateException("You already rated this gym")
            }

            val oldAvg = (gymSnap.getDouble("avgRating") ?: 0.0)
            val oldCount = (gymSnap.getLong("ratingCount") ?: 0L)

            //Izracunavanje novog proseka nakon dodavanja nove ocene
            val newCount = oldCount + 1
            val newAvg = ((oldAvg * oldCount) + value) / newCount.toDouble()

            //cuvanje pojedinacne ocene u ratings subkolekciji
            tx.set(
                ratingRef,
                mapOf(
                    "value" to value,
                    "authorUid" to uid,
                    "authorUsername" to (me?.fullName?.ifBlank { me.email } ?: ""),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            //Azuriranje prosecne ocene i broja ocena na gym dokumentu
            tx.update(
                gymRef,
                mapOf(
                    "avgRating" to newAvg,
                    "ratingCount" to newCount
                )
            )

            //Dodavanje poena korisniku za ostavljenu ocenu
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

        //Proverava da li je trenutni korisnik vec dao ocenu na teretani
        val snap = db.collection("gyms").document(gymId)
            .collection("ratings").document(uid)
            .get().await()
        return snap.exists()
    }
}
