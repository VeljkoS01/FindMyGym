package com.example.findmygym

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.findmygym.databinding.ActivityRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class Register : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var photoUri: Uri? = null // For photo upload

    // Register for activity result to pick photo from gallery
    private val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            photoUri = result.data?.data
            Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Photo selection failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Pick photo logic (from gallery)
        binding.uploadPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickPhotoLauncher.launch(intent)
        }

        binding.registerButton.setOnClickListener {
            val email = binding.registerEmail.text.toString()
            val password = binding.registerPassword.text.toString()
            val confirmPassword = binding.registerConfirmPassword.text.toString()
            val username = binding.registerUsername.text.toString()
            val name = binding.registerName.text.toString()
            val surname = binding.registerSurname.text.toString()
            val phone = binding.registerPhone.text.toString()

            // Provera da li su svi podaci uneti
            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() &&
                username.isNotEmpty() && name.isNotEmpty() && surname.isNotEmpty() && phone.isNotEmpty() && photoUri != null
            ) {
                if (password == confirmPassword) {
                    // Kreiranje korisnika u Firebase Authentication
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            val userId = firebaseAuth.currentUser?.uid
                            val user = hashMapOf(
                                "username" to username,
                                "name" to name,
                                "surname" to surname,
                                "phone" to phone,
                                "email" to email
                            )
                            // Upload fotografije
                            uploadPhoto(userId, user)
                        } else {
                            Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (photoUri == null) {
                    Toast.makeText(this, "Please upload a photo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Redirect na Login stranu
        binding.loginRedirectText.setOnClickListener {
            val loginIntent = Intent(this, Login::class.java)
            startActivity(loginIntent)
        }
    }

    private fun uploadPhoto(userId: String?, user: Map<String, Any>) {
        val photoRef = storage.reference.child("profile_photos/$userId/${UUID.randomUUID()}")
        photoUri?.let { uri ->
            photoRef.putFile(uri).addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener { url ->
                    saveUserData(userId, user.plus("photoUrl" to url.toString()))
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserData(userId: String?, user: Map<String, Any>) {
        userId?.let {
            FirebaseFirestore.getInstance().collection("users").document(it).set(user)
                .addOnSuccessListener {
                    Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "User registration failed", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
