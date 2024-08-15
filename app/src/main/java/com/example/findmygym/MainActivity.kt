package com.example.findmygym

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userPhoneTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Bind views
        profileImageView = findViewById(R.id.profile_image)
        userNameTextView = findViewById(R.id.user_name)
        userEmailTextView = findViewById(R.id.user_email)
        userPhoneTextView = findViewById(R.id.user_phone)

        // Load user data
        loadUserData()
    }

    private fun loadUserData() {
        val userId = firebaseAuth.currentUser?.uid
        userId?.let {
            firestore.collection("users").document(it).get().addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: "N/A"
                    val surname = document.getString("surname") ?: "N/A"
                    val email = document.getString("email") ?: "N/A"
                    val phone = document.getString("phone") ?: "N/A"
                    val photoUrl = document.getString("photoUrl")

                    // Set data to views
                    userNameTextView.text = "$name $surname"
                    userEmailTextView.text = email
                    userPhoneTextView.text = "Phone: $phone"

                    // Load profile image
                    if (photoUrl != null) {
                        Glide.with(this)
                            .load(Uri.parse(photoUrl))
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.default_profile_image)
                    }
                }
            }.addOnFailureListener {
                // Handle error
            }
        }
    }
}
