package com.rideconnect.app.models

data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val senderName: String = "Rider",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val profilePictureUrl: String = ""
)
