package com.rideconnect.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.rideconnect.app.models.ChatMessage
import com.rideconnect.app.models.UserProfile
import com.rideconnect.app.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = FirebaseRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private var currentRideId: String? = null

    fun setRideId(rideId: String) {
        if (currentRideId == rideId) return
        currentRideId = rideId
        viewModelScope.launch {
            repository.observeChatMessages(rideId).collect {
                _messages.value = it
            }
        }
    }

    fun sendMessage(text: String, profile: UserProfile?) {
        val rideId = currentRideId ?: return
        if (text.isBlank()) return
        
        val currentUser = auth.currentUser ?: return
        
        val message = ChatMessage(
            senderUid = currentUser.uid,
            senderName = profile?.displayName ?: "Rider",
            text = text,
            profilePictureUrl = profile?.profilePictureUrl ?: ""
        )
        
        viewModelScope.launch {
            repository.sendMessage(rideId, message)
        }
    }
}
