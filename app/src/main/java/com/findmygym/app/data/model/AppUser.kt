package com.findmygym.app.data.model

data class AppUser(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val phone: String = "",
    val points: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLat: Double? = null,
    val lastLng: Double? = null,
    val lastLocationAt: Long? = null
)
