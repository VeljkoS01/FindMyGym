package com.findmygym.app.data.model

data class GymComment(
    val id: String = "",
    val gymId: String = "",
    val authorUid: String = "",
    val authorUsername: String = "",
    val text: String = "",
    val createdAt: Long = System.currentTimeMillis()
)