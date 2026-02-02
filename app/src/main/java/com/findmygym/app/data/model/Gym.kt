package com.findmygym.app.data.model

data class Gym(
    val id: String = "",
    val name: String = "",
    val type: String = "Gym",
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val authorUid: String = "",
    val authorUsername: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val avgRating: Double = 0.0,
    val ratingCount: Int = 0
)