package com.rideconnect.app.models

// NOTE: Do NOT add @Parcelize or Parcelable here — Firebase cannot deserialize Parcelable classes.
// The UI uses a custom saver (see StartRideScreen) to persist this across navigation.
data class Waypoint(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isReached: Boolean = false
)
