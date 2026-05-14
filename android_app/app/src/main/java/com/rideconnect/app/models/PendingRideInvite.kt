package com.rideconnect.app.models

data class PendingRideInvite(
    val rideId: String,
    val rideName: String,
    val ownerUid: String
)
