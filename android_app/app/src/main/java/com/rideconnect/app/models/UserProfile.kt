package com.rideconnect.app.models

data class UserProfile(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "Rider",
    val memberSince: Long = System.currentTimeMillis(),
    val category: String = "AMATEUR", // AMATEUR or PRO
    val totalDistanceKm: Double = 0.0,
    val monthlyElevationGain: Double = 0.0,
    val averageSpeedKmh: Double = 0.0,
    val autoPauseEnabled: Boolean = true,
    val isMetric: Boolean = true,
    val mapStyleDarkMode: Boolean = true,
    val pushAlertsEnabled: Boolean = false,
    val groupMessagesEnabled: Boolean = true,
    val sosAlertsEnabled: Boolean = true,
    val gearTitle: String = "Standard Road Bike",
    val gearSubtitle: String = "Aluminum Frame",
    
    // Detailed Profile Fields
    val phoneNumber: String = "",
    val gender: String = "Not Specified",
    val bloodGroup: String = "Not Specified",
    val dob: String = "",
    val location: String = "",
    val riderId: String = "",
    val profilePictureUrl: String = ""
)
