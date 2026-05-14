package com.rideconnect.app.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.rideconnect.app.models.ChatMessage
import com.rideconnect.app.models.Rider
import com.rideconnect.app.models.UserProfile
import com.rideconnect.app.repository.FirebaseRepository
import com.rideconnect.app.repository.JoinRideCheck
import com.rideconnect.app.service.LocationTracker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class RideViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FirebaseRepository()
    private val locationTracker = LocationTracker(application)
    private val auth = FirebaseAuth.getInstance()

    private val _riders = MutableStateFlow<List<Rider>>(emptyList())
    // Throttled version for UI to save battery/CPU
    private val _throttledRiders = MutableStateFlow<List<Rider>>(emptyList())
    val riders = _throttledRiders.asStateFlow()

    private val _currentRideId = MutableStateFlow<String?>(null)
    val currentRideId = _currentRideId.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline = _isOnline.asStateFlow()

    private val _distanceToDestinationKm = MutableStateFlow<Float?>(null)
    val distanceToDestinationKm = _distanceToDestinationKm.asStateFlow()

    private val _isDayMode = MutableStateFlow(false)
    val isDayMode = _isDayMode.asStateFlow()

    private var lastFirebaseUploadTime = 0L
    private var lastLocation: android.location.Location? = null

    // Real-Time Metrics
    private val _currentSpeedKmh = MutableStateFlow(0f)
    val currentSpeedKmh = _currentSpeedKmh.asStateFlow()

    private val _totalDistanceKm = MutableStateFlow(0f)
    val totalDistanceKm = _totalDistanceKm.asStateFlow()

    private val _rideInvites = MutableStateFlow<List<String>>(emptyList())
    val rideInvites = _rideInvites.asStateFlow()

    private val _pendingInvites = MutableStateFlow<List<com.rideconnect.app.models.PendingRideInvite>>(emptyList())
    val pendingInvites = _pendingInvites.asStateFlow()

    private val _rideInviteLabels = MutableStateFlow<Map<String, String>>(emptyMap())
    val rideInviteLabels = _rideInviteLabels.asStateFlow()

    private val _rideActionState = MutableStateFlow<RideActionState>(RideActionState.Idle)
    val rideActionState = _rideActionState.asStateFlow()

    private val _currentWaypoints = MutableStateFlow<List<com.rideconnect.app.models.Waypoint>>(emptyList())
    val currentWaypoints = _currentWaypoints.asStateFlow()

    private val _rideDestination = MutableStateFlow("")
    val rideDestination = _rideDestination.asStateFlow()

    private val _destLat = MutableStateFlow(0.0)
    val destLat = _destLat.asStateFlow()

    private val _destLng = MutableStateFlow(0.0)
    val destLng = _destLng.asStateFlow()

    private val _currentLocation = MutableStateFlow<com.google.android.gms.maps.model.LatLng?>(null)
    val currentLocation = _currentLocation.asStateFlow()


    private var locationJob: Job? = null
    private var ridersJob: Job? = null
    private var invitesJob: Job? = null
    private var metaJob: Job? = null
    private var profileJob: Job? = null

    private val _rideHistory = MutableStateFlow<List<com.rideconnect.app.models.RideRecord>>(emptyList())
    val rideHistory = _rideHistory.asStateFlow()

    private val _rideDurationMillis = MutableStateFlow(0L)
    val rideDurationMillis = _rideDurationMillis.asStateFlow()

    private val _followMode = MutableStateFlow(FollowMode.FOLLOW_ME)
    val followMode = _followMode.asStateFlow()

    private val _timeToDestMinutes = MutableStateFlow<Int?>(null)
    val timeToDestMinutes = _timeToDestMinutes.asStateFlow()

    private var timerJob: Job? = null

    private var topSpeed = 0f
    private var rideStartTime = 0L
    private val currentUserProfile = MutableStateFlow<UserProfile?>(null)

    init {
        loadHistory()
        observeCurrentUserProfile()
        setupNetworkMonitoring()
        startUiThrottling()
    }

    fun toggleDayMode() {
        _isDayMode.value = !_isDayMode.value
    }

    private fun setupNetworkMonitoring() {
        val connectivityManager = getApplication<Application>().getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val networkRequest = android.net.NetworkRequest.Builder()
            .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) { _isOnline.value = true }
            override fun onLost(network: android.net.Network) { _isOnline.value = false }
        })
    }

    private fun startUiThrottling() {
        viewModelScope.launch {
            while (true) {
                _throttledRiders.value = _riders.value
                // In a real app, calculate distance to destination here
                delay(2000)
            }
        }
    }

    private fun loadHistory() {
        auth.currentUser?.uid?.let { uid ->
            viewModelScope.launch {
                repository.observeRideHistory(uid).collect {
                    _rideHistory.value = it
                }
            }
            viewModelScope.launch {
                repository.observeUserPendingInvites(uid).collect { pendingList ->
                    _pendingInvites.value = pendingList
                }
            }
        }
    }

    val riderId: String
        get() = auth.currentUser?.uid.orEmpty()

    val riderName: String
        get() {
            val profileName = currentUserProfile.value?.displayName?.trim().orEmpty()
            return if (profileName.isNotBlank() && profileName != "Rider") {
                profileName
            } else {
                auth.currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Unknown Rider"
            }
        }

    private val riderPhoneNumber: String
        get() = currentUserProfile.value?.phoneNumber?.trim().orEmpty()

    private fun observeCurrentUserProfile() {
        val currentUser = auth.currentUser ?: return
        profileJob?.cancel()
        profileJob = viewModelScope.launch {
            repository.observeUserProfile(currentUser.uid, currentUser.email).collect { profile ->
                currentUserProfile.value = profile
            }
        }
    }

    fun endRide() {
        if (_currentRideId.value == null) return
        val uid = auth.currentUser?.uid ?: return
        
        val durationMin = ((System.currentTimeMillis() - rideStartTime) / 60000).toInt()
        val avgSpeed = if (durationMin > 0) (_totalDistanceKm.value / (durationMin / 60f)) else 0f
        
        val record = com.rideconnect.app.models.RideRecord(
            name = "Ride at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
            date = System.currentTimeMillis(),
            distanceKm = _totalDistanceKm.value.toDouble(),
            topSpeedKmh = topSpeed,
            avgSpeedKmh = avgSpeed,
            durationMinutes = durationMin,
            destination = "",
            waypoints = _currentWaypoints.value
        )
        
        viewModelScope.launch {
            repository.saveRideRecord(uid, record)
            // Cleanup active ride
            _currentRideId.value = null
            locationJob?.cancel()
            ridersJob?.cancel()
            invitesJob?.cancel()
            _rideActionState.value = RideActionState.Idle
        }
    }

    fun startRide(rideName: String, destination: String, destLat: Double, destLng: Double, invitedEmails: List<String>, waypoints: List<com.rideconnect.app.models.Waypoint>, isPublic: Boolean) {
        _rideDestination.value = destination
        _destLat.value = destLat
        _destLng.value = destLng
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _rideActionState.value = RideActionState.Error("You must be logged in to start a secure ride")
            return
        }
        if (rideName.isBlank()) {
            _rideActionState.value = RideActionState.Error("Ride name is required")
            return
        }

        viewModelScope.launch {
            _rideActionState.value = RideActionState.Loading
            try {
                val inviteResolution = if (invitedEmails.isNotEmpty()) {
                    repository.resolveInviteEmails(invitedEmails)
                } else {
                    com.rideconnect.app.repository.InviteResolutionResult(emptyList(), emptyList())
                }

                if (inviteResolution.missingEmails.isNotEmpty()) {
                    _rideActionState.value = RideActionState.Error(
                        "Unknown rider emails: ${inviteResolution.missingEmails.joinToString()}"
                    )
                    return@launch
                }

                // Add a timeout to the creation process
                val newRideId = kotlinx.coroutines.withTimeout(10000) {
                    repository.createRide(
                        ownerUid = uid,
                        rideName = rideName.trim(),
                        destination = destination.trim(),
                        destLat = destLat,
                        destLng = destLng,
                        invitedUids = inviteResolution.resolvedUids,
                        waypoints = waypoints,
                        isPublic = isPublic
                    )
                }
                
                _currentWaypoints.value = waypoints
                activateRide(newRideId)
                _rideActionState.value = RideActionState.Success(newRideId)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _rideActionState.value = RideActionState.Error("Connection timeout. Please check your internet.")
            } catch (e: Exception) {
                _rideActionState.value = RideActionState.Error(e.localizedMessage ?: "Unable to start ride")
            }
        }
    }

    fun joinRide(rideId: String) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            _rideActionState.value = RideActionState.Error("You must be logged in to join a secure ride")
            return
        }
        if (rideId.isBlank()) {
            _rideActionState.value = RideActionState.Error("Ride ID is required")
            return
        }

        viewModelScope.launch {
            _rideActionState.value = RideActionState.Loading
            try {
                when (repository.canJoinRide(rideId.trim(), uid)) {
                    JoinRideCheck.NotFound -> {
                        _rideActionState.value = RideActionState.Error("Ride not found")
                    }
                    JoinRideCheck.NotInvited -> {
                        _rideActionState.value = RideActionState.Error("You are not invited to this ride")
                    }
                    JoinRideCheck.Invited -> {
                        repository.joinRide(rideId.trim(), uid)
                        activateRide(rideId.trim())
                        _rideActionState.value = RideActionState.Success(rideId.trim())
                    }
                    JoinRideCheck.AlreadyMember -> {
                        activateRide(rideId.trim())
                        _rideActionState.value = RideActionState.Success(rideId.trim())
                    }
                }
            } catch (e: Exception) {
                _rideActionState.value = RideActionState.Error(e.localizedMessage ?: "Unable to join ride")
            }
        }
    }

    private fun startLocationTracking(rideId: String) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            locationTracker.getLocationUpdates().collect { location ->
                val speedMps = if (location.hasSpeed()) location.speed else 0f
                val speedKmh = speedMps * 3.6f
                _currentSpeedKmh.value = speedKmh
                if (speedKmh > topSpeed) topSpeed = speedKmh

                _currentLocation.value = com.google.android.gms.maps.model.LatLng(location.latitude, location.longitude)

                lastLocation?.let { prevLoc ->
                    val distanceMeters = prevLoc.distanceTo(location)
                    _totalDistanceKm.value += (distanceMeters / 1000f)
                }
                
                // Adaptive Throttling: Only upload if we've moved 5m or 5s have passed
                val timeSinceLastUpload = System.currentTimeMillis() - lastFirebaseUploadTime
                val distanceMoved = lastLocation?.let { it.distanceTo(location) } ?: 10f

                if (timeSinceLastUpload > 5000 || distanceMoved > 5) {
                    // Update ETA calculation
                    val dist = _distanceToDestinationKm.value
                    if (dist != null && dist > 0) {
                        if (speedKmh > 5f) {
                            val hours = dist / speedKmh
                            _timeToDestMinutes.value = (hours * 60).toInt()
                        } else {
                            _timeToDestMinutes.value = null
                        }
                    } else {
                        _timeToDestMinutes.value = null
                    }

                    if (riderId.isNotBlank()) {
                        repository.updateRiderLocation(
                            rideId = rideId,
                            riderId = riderId,
                            lat = location.latitude,
                            lng = location.longitude,
                            displayName = riderName,
                            phoneNumber = riderPhoneNumber,
                            speedKmh = speedKmh
                        )
                        lastFirebaseUploadTime = System.currentTimeMillis()
                        lastLocation = location
                    }
                }
            }
        }
    }

    private fun observeRiders(rideId: String) {
        ridersJob?.cancel()
        ridersJob = viewModelScope.launch {
            repository.observeRiders(rideId).collect {
                _riders.value = it
            }
        }
    }

    private fun observeInvites(rideId: String) {
        invitesJob?.cancel()
        invitesJob = viewModelScope.launch {
            repository.observeRideInvites(rideId).collect {
                _rideInvites.value = it
                _rideInviteLabels.value = repository.resolveInviteLabels(it)
            }
        }
    }

    private fun observeRideMeta(rideId: String) {
        metaJob?.cancel()
        metaJob = viewModelScope.launch {
            repository.observeRideMeta(rideId).collect { meta ->
                meta?.let {
                    _currentWaypoints.value = it.waypoints
                    _rideDestination.value = it.destination
                    _destLat.value = it.destLat
                    _destLng.value = it.destLng
                }
            }
        }
    }

    fun leaveRide() {
        locationJob?.cancel()
        ridersJob?.cancel()
        invitesJob?.cancel()
        metaJob?.cancel()
        timerJob?.cancel()
        
        _currentRideId.value = null
        _riders.value = emptyList()
        _rideActionState.value = RideActionState.Idle
    }

    fun triggerSos() {
        val rideId = _currentRideId.value ?: return
        if (riderId.isBlank()) {
            _rideActionState.value = RideActionState.Error("You must be logged in to use SOS")
            return
        }
        viewModelScope.launch {
            try {
                repository.triggerSos(rideId, riderId)
            } catch (e: Exception) {
                _rideActionState.value = RideActionState.Error(e.localizedMessage ?: "Unable to broadcast SOS")
            }
        }
    }

    fun sendQuickMessage(text: String) {
        val rideId = _currentRideId.value ?: return
        val uid = riderId
        if (uid.isBlank()) return
        
        viewModelScope.launch {
            val msg = com.rideconnect.app.models.ChatMessage(
                senderUid = uid,
                senderName = riderName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            repository.sendMessage(rideId, msg)
        }
    }

    fun setFollowMode(mode: FollowMode) {
        _followMode.value = mode
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                _rideDurationMillis.value = System.currentTimeMillis() - rideStartTime
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun resetRideActionState() {
        _rideActionState.value = RideActionState.Idle
    }

    private fun activateRide(rideId: String) {
        _totalDistanceKm.value = 0f
        _currentSpeedKmh.value = 0f
        topSpeed = 0f
        rideStartTime = System.currentTimeMillis()
        lastLocation = null
        _currentLocation.value = null
        _currentRideId.value = rideId
        startLocationTracking(rideId)
        observeRiders(rideId)
        observeInvites(rideId)
        observeRideMeta(rideId)
        startTimer()
    }

    override fun onCleared() {
        super.onCleared()
        locationJob?.cancel()
        ridersJob?.cancel()
        invitesJob?.cancel()
        metaJob?.cancel()
        timerJob?.cancel()
        profileJob?.cancel()
    }

    fun joinRide(rideId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@launch
            _rideActionState.value = RideActionState.Loading
            
            // Check if ride exists
            when (repository.canJoinRide(rideId, uid)) {
                com.rideconnect.app.repository.JoinRideCheck.NotFound -> {
                    _rideActionState.value = RideActionState.Error("Ride not found. Check the ID.")
                    return@launch
                }
                else -> {
                    // Just join it if they have the ID
                    repository.joinRide(rideId, uid)
                    activateRide(rideId)
                    _rideActionState.value = RideActionState.Success(rideId)
                    onComplete()
                }
            }
        }
    }

    fun acceptInvite(invite: com.rideconnect.app.models.PendingRideInvite, onComplete: () -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@launch
            // remove pending invite
            repository.removePendingInvite(uid, invite.rideId)
            // join the ride
            repository.joinRide(invite.rideId, uid)
            activateRide(invite.rideId)
            onComplete()
        }
    }

    fun declineInvite(invite: com.rideconnect.app.models.PendingRideInvite) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val uid = auth.currentUser?.uid ?: return@launch
            repository.removePendingInvite(uid, invite.rideId)
        }
    }
}

enum class FollowMode {
    FOLLOW_ME,
    FOLLOW_LEADER
}

sealed class RideActionState {
    object Idle : RideActionState()
    object Loading : RideActionState()
    data class Success(val rideId: String) : RideActionState()
    data class Error(val message: String) : RideActionState()
}
