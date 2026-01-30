package com.findmygym.app.data.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val points: Int = 0
)