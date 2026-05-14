package com.rideconnect.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rideconnect.app.models.UserProfile
import com.rideconnect.app.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile = _userProfile.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                repository.observeUserProfile(currentUser.uid, currentUser.email).collect { profile ->
                    _userProfile.value = profile
                }
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        repository.updateUserProfile(profile)
    }

    suspend fun uploadAndSaveProfile(profile: UserProfile, imageUri: android.net.Uri?) {
        var finalProfile = profile
        if (imageUri != null && !imageUri.toString().startsWith("http")) {
            val downloadUrl = repository.uploadProfilePicture(profile.uid, imageUri)
            finalProfile = profile.copy(profilePictureUrl = downloadUrl)
        }
        updateProfile(finalProfile)
    }

    fun toggleAutoPause(enabled: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(autoPauseEnabled = enabled)) }
    }
    
    fun toggleUnits(isMetric: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(isMetric = isMetric)) }
    }

    fun togglePushAlerts(enabled: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(pushAlertsEnabled = enabled)) }
    }

    fun toggleGroupMessages(enabled: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(groupMessagesEnabled = enabled)) }
    }

    fun toggleSosAlerts(enabled: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(sosAlertsEnabled = enabled)) }
    }

    fun toggleMapStyle(darkMode: Boolean) {
        _userProfile.value?.let { updateProfile(it.copy(mapStyleDarkMode = darkMode)) }
    }

    fun updateDisplayName(name: String) {
        _userProfile.value?.let { updateProfile(it.copy(displayName = name)) }
    }
}
