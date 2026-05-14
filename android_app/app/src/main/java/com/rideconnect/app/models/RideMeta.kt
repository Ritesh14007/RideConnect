package com.rideconnect.app.models

data class RideMeta(
    val ownerUid: String = "",
    val name: String = "",
    val destination: String = "",
    val destLat: Double = 0.0,
    val destLng: Double = 0.0,
    val createdAt: Long = 0L,
    val status: String = "active",
    val waypoints: List<Waypoint> = emptyList(),
    val isPublic: Boolean = true
)
