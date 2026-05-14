package com.rideconnect.app.models

data class RideRecord(
    val id: String = "",
    val name: String = "",
    val date: Long = System.currentTimeMillis(),
    val distanceKm: Double = 0.0,
    val topSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val durationMinutes: Int = 0,
    val destination: String = "",
    val waypoints: List<Waypoint> = emptyList()
)
