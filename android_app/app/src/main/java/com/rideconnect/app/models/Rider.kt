package com.rideconnect.app.models

data class Rider(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isSos: Boolean = false,
    val speedKmh: Float = 0f,
    val lastUpdated: Long = 0L
)
