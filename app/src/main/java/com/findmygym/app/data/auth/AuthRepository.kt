package com.findmygym.app.data.auth

import android.util.Log
import com.findmygym.app.data.model.AppUser
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class AuthRepository(
    //FirebaseAuth sluzi za login, register i logout korisnika
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    // Firestore sluzi za cuvanje dodatnih podataka o korisniku i gym-ovima
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    //Da li je prijavljen korisnk
    fun isLoggedIn(): Boolean = auth.currentUser != null
    //Vraca Uid trenutnog korisnika
    fun currentUid(): String? = auth.currentUser?.uid
    fun logout() = auth.signOut()

    suspend fun login(emailRaw: String, password: String) {
        val email = emailRaw.trim()
        if (email.isBlank()) throw Exception("Enter your email")

        //Firebase prijava korisnika
        auth.signInWithEmailAndPassword(email, password).await()

        //Ukoliko je doslo do greske, pravi se minimalan profil da aplikacije ne pukne
        ensureUserDocExists()
    }

    suspend fun register(
        fullName: String,
        emailRaw: String,
        password: String,
        phone: String
    ) {
        val email = emailRaw.trim()

        //Validacija unetih podataka pre registracije
        if (fullName.trim().isBlank()) throw Exception("Enter your full name")
        if (email.isBlank()) throw Exception("Enter your email")
        if (password.length < 6) throw Exception("Password must be at least 6 characters")
        if (phone.trim().isBlank()) throw Exception("Enter your phone number")

        //Kreiranje auth naloga u Firebase Authentication
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception("Registration failed. Try again.")

        //Kreiranje profila za Firestore bazu
        val profile = AppUser(
            uid = user.uid,
            email = email,
            fullName = fullName.trim(),
            phone = phone.trim(),
            points = 0
        )

        //Dodavanje profila u Firestore bazu
        db.collection("users").document(user.uid).set(profile).await()
    }


    suspend fun getMyProfile(): AppUser? {
        val uid = currentUid() ?: return null

        val ref = db.collection("users").document(uid)
        val snap = ref.get().await()

        //Ako korisnik ne postoji u bazi, pravi se minimalna verzija profila
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
        //Pretvara Firestore dokument u AppUser objekat
        return snap.toObject(AppUser::class.java)
    }

    suspend fun updateMyLocation(lat: Double, lng: Double) {
        val uid = currentUid() ?: return

        //Upisivanje poslednje lokacije korisnika zajedno za vremenom
        db.collection("users").document(uid).set(
            mapOf(
                "lastLat" to lat,
                "lastLng" to lng,
                "lastLocationAt" to System.currentTimeMillis()
            ),
            SetOptions.merge()
        ).await()
    }

    //Ponovna autentifikacija prilikom brisanja naloga
    suspend fun reauthenticateWithPassword(password: String) {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val email = user.email ?: throw Exception("No email for current user")

        val cred = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(cred).await()
    }

    /* Brisem korisnika kao i sve teretane koje je on napravio, ocene i komentare tim teretanama
    i sve ocene i komentare koje je korisnik ostavio na drugim teretanama */
    suspend fun deleteAccountAndData() {
        val user = auth.currentUser ?: throw Exception("Not logged in")
        val uid = user.uid

        //Moji gyms
        val myGymsSnap = db.collection("gyms")
            .whereEqualTo("authorUid", uid)
            .get(Source.SERVER).await()

        val myGymRefs = myGymsSnap.documents.map { it.reference }
        val myGymIds = myGymsSnap.documents.map { it.id }.toHashSet()

        //Sve gyms (da bih ocistio moje comments/ratings svuda)
        val allGymsSnap = db.collection("gyms").get(Source.SERVER).await()

        //1) Obrisi moje ratings na tudjim gyms + preracunaj prosecnu ocenu
        try {
            for (gymDoc in allGymsSnap.documents) {
                val gymId = gymDoc.id
                val gymRef = gymDoc.reference

                //Moje gyms brisem kasnije cele
                if (myGymIds.contains(gymId)) continue

                // Da li postoji moja ocena na ovoj teretani
                val ratingRef = gymRef.collection("ratings").document(uid)
                val ratingSnap = ratingRef.get(Source.SERVER).await()
                if (!ratingSnap.exists()) continue

                //U istoj transakciji se brise ocena i racuna nova prosecna ocena
                db.runTransaction { tx ->
                    val gSnap = tx.get(gymRef)
                    val rSnap = tx.get(ratingRef)

                    if (!gSnap.exists() || !rSnap.exists()) return@runTransaction null

                    val value = (rSnap.get("value") as? Number)?.toInt() ?: 0
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

        //2) Brisem moje comments na svim gym-ovima
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

        // 3) Brisem moje gym-ove zajedno sa njihovim ratings i comments
        try {
            for (gymRef in myGymRefs) {
                deleteCollectionInBatches(gymRef.collection("ratings"))
                deleteCollectionInBatches(gymRef.collection("comments"))
                gymRef.delete().await()
            }
        } catch (e: Exception) {
            throw Exception("Delete failed at: my gyms cleanup. ${e.message}")
        }

        // 4)Birsanje users/{uid}
        try {
            db.collection("users").document(uid).delete().await()
        } catch (e: Exception) {
            throw Exception("Delete failed at: user doc. ${e.message}")
        }

        // 5)Brisanje Auth user
        try {
            user.delete().await()
        } catch (e: Exception) {
            throw Exception("Delete failed at: auth user. ${e.message}")
        }
    }

    //Brisanje u batch-evima
    private suspend fun deleteCollectionInBatches(col: CollectionReference, batchSize: Int = 450) {
        while (true) {
            val snap = col.limit(batchSize.toLong()).get(Source.SERVER).await()
            if (snap.isEmpty) break

            val batch = db.batch()
            for (d in snap.documents) batch.delete(d.reference)
            batch.commit().await()
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
