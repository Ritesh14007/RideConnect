package com.rideconnect.app.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rideconnect.app.models.Rider
import com.rideconnect.app.models.RideMeta
import com.rideconnect.app.models.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import com.google.firebase.storage.FirebaseStorage
import android.net.Uri

import com.rideconnect.app.models.ChatMessage
import com.rideconnect.app.models.PendingRideInvite

import com.rideconnect.app.models.RideRecord

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance("https://rideconnect-90e39-default-rtdb.firebaseio.com/").reference
    private val storage = FirebaseStorage.getInstance().reference

    fun observeRideHistory(uid: String): Flow<List<RideRecord>> = callbackFlow {
        val historyRef = database.child("users").child(uid).child("history")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val records = snapshot.children.mapNotNull { it.getValue(RideRecord::class.java) }
                    .sortedByDescending { it.date }
                trySend(records)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        historyRef.addValueEventListener(listener)
        awaitClose { historyRef.removeEventListener(listener) }
    }

    suspend fun saveRideRecord(uid: String, record: RideRecord) {
        val recordRef = database.child("users").child(uid).child("history").push()
        recordRef.setValue(record.copy(id = recordRef.key ?: "")).await()
    }

    fun observeChatMessages(rideId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatRef = database.child("rides").child(rideId).child("chat")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    .sortedBy { it.timestamp }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        chatRef.addValueEventListener(listener)
        awaitClose { chatRef.removeEventListener(listener) }
    }

    suspend fun sendMessage(rideId: String, message: ChatMessage) {
        val chatRef = database.child("rides").child(rideId).child("chat").push()
        chatRef.setValue(message.copy(id = chatRef.key ?: "")).await()
    }

    suspend fun uploadProfilePicture(uid: String, imageUri: Uri): String {
        val fileRef = storage.child("profile_pictures/$uid.jpg")
        fileRef.putFile(imageUri).await()
        return fileRef.downloadUrl.await().toString()
    }

    private fun encodeEmail(email: String): String =
        email.trim().lowercase().replace(".", ",")

    fun observeRiders(rideId: String): Flow<List<Rider>> = callbackFlow {
        val ridersRef = database.child("rides").child(rideId).child("riders")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val riders = snapshot.children.mapNotNull { it.getValue(Rider::class.java) }
                trySend(riders)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        ridersRef.addValueEventListener(listener)
        awaitClose { ridersRef.removeEventListener(listener) }
    }

    fun observeRideInvites(rideId: String): Flow<List<String>> = callbackFlow {
        val invitesRef = database.child("rides").child(rideId).child("invites")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inviteUids = snapshot.children.mapNotNull { child ->
                    child.key?.takeIf { child.getValue(Boolean::class.java) == true }
                }
                trySend(inviteUids)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        invitesRef.addValueEventListener(listener)
        awaitClose { invitesRef.removeEventListener(listener) }
    }

    suspend fun resolveInviteLabels(inviteUids: List<String>): Map<String, String> {
        val labels = mutableMapOf<String, String>()
        inviteUids
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { uid ->
                val profile = database.child("users").child(uid).get().await().getValue(UserProfile::class.java)
                val label = when {
                    !profile?.displayName.isNullOrBlank() && profile!!.displayName != "Rider" -> profile.displayName
                    !profile?.email.isNullOrBlank() -> profile!!.email
                    else -> uid
                }
                labels[uid] = label
            }
        return labels
    }

    suspend fun createRide(
        ownerUid: String,
        rideName: String,
        destination: String,
        destLat: Double,
        destLng: Double,
        invitedUids: List<String>,
        waypoints: List<com.rideconnect.app.models.Waypoint> = emptyList(),
        isPublic: Boolean = true
    ): String {
        val rideId = java.util.UUID.randomUUID().toString().substring(0, 6).uppercase()
        val rideRef = database.child("rides").child(rideId)
        val meta = RideMeta(
            ownerUid = ownerUid,
            name = rideName,
            destination = destination,
            destLat = destLat,
            destLng = destLng,
            createdAt = System.currentTimeMillis(),
            status = "active",
            waypoints = waypoints,
            isPublic = isPublic
        )
        val updates = mutableMapOf<String, Any>(
            "meta" to meta,
            "members/$ownerUid" to true
        )
        invitedUids
            .filter { it.isNotBlank() && it != ownerUid }
            .distinct()
            .forEach { invitedUid ->
                updates["invites/$invitedUid"] = true
                // Also write to the invitee's personal index so they see it on Home screen
                database.child("userInvites").child(invitedUid).child(rideId).setValue(
                    mapOf("rideName" to rideName, "ownerUid" to ownerUid, "rideId" to rideId)
                )
            }
        rideRef.updateChildren(updates).await()
        return rideId
    }

    /** Observe all pending ride invites for a user (shown on Home screen) */
    fun observeUserPendingInvites(uid: String): Flow<List<PendingRideInvite>> = callbackFlow {
        val ref = database.child("userInvites").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val invites = snapshot.children.mapNotNull { child ->
                    val rideId   = child.child("rideId").getValue(String::class.java) ?: return@mapNotNull null
                    val rideName = child.child("rideName").getValue(String::class.java) ?: "Unknown Ride"
                    val ownerUid = child.child("ownerUid").getValue(String::class.java) ?: ""
                    PendingRideInvite(rideId = rideId, rideName = rideName, ownerUid = ownerUid)
                }
                trySend(invites)
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Remove a pending invite after user accepts or declines */
    suspend fun removePendingInvite(uid: String, rideId: String) {
        database.child("userInvites").child(uid).child(rideId).removeValue().await()
    }

    suspend fun resolveInviteEmails(inviteEmails: List<String>): InviteResolutionResult {
        val resolvedUids = mutableListOf<String>()
        val missingEmails = mutableListOf<String>()

        inviteEmails
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { email ->
                val uid = database.child("emailIndex").child(encodeEmail(email)).get().await().getValue(String::class.java)
                if (uid.isNullOrBlank()) {
                    missingEmails += email
                } else {
                    resolvedUids += uid
                }
            }

        return InviteResolutionResult(
            resolvedUids = resolvedUids.distinct(),
            missingEmails = missingEmails
        )
    }

    suspend fun canJoinRide(rideId: String, uid: String): JoinRideCheck {
        val rideRef = database.child("rides").child(rideId)
        val metaSnapshot = rideRef.child("meta").get().await()
        if (!metaSnapshot.exists()) {
            return JoinRideCheck.NotFound
        }

        val isMember = rideRef.child("members").child(uid).get().await().getValue(Boolean::class.java) == true
        if (isMember) {
            return JoinRideCheck.AlreadyMember
        }

        // Check if ride is public - anyone can join public rides
        val isPublic = metaSnapshot.child("isPublic").getValue(Boolean::class.java) != false
        if (isPublic) {
            return JoinRideCheck.Invited // Allow anyone to join public rides
        }

        // For private rides, check if user is pre-invited
        val isInvited = rideRef.child("invites").child(uid).get().await().getValue(Boolean::class.java) == true
        return if (isInvited) JoinRideCheck.Invited else JoinRideCheck.NotInvited
    }

    suspend fun joinRide(rideId: String, uid: String) {
        val rideRef = database.child("rides").child(rideId)
        rideRef.child("members").child(uid).setValue(true).await()
    }

    suspend fun updateRiderLocation(
        rideId: String,
        riderId: String,
        lat: Double,
        lng: Double,
        isSos: Boolean = false,
        displayName: String = "Rider",
        phoneNumber: String = "",
        speedKmh: Float = 0f
    ) {
        val riderRef = database.child("rides").child(rideId).child("riders").child(riderId)
        val update = mapOf(
            "id" to riderId,
            "name" to displayName,
            "phoneNumber" to phoneNumber,
            "lat" to lat,
            "lng" to lng,
            "isSos" to isSos,
            "speedKmh" to speedKmh,
            "lastUpdated" to System.currentTimeMillis()
        )
        riderRef.updateChildren(update).await()
    }

    suspend fun triggerSos(rideId: String, riderId: String) {
        val riderRef = database.child("rides").child(rideId).child("riders").child(riderId)
        riderRef.child("isSos").setValue(true).await()
    }

    fun observeUserProfile(uid: String, email: String? = null): Flow<UserProfile?> = callbackFlow {
        val userRef = database.child("users").child(uid)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val profile = snapshot.getValue(UserProfile::class.java)
                    val normalizedEmail = email?.trim()?.lowercase().orEmpty()
                    if (profile != null && normalizedEmail.isNotBlank() && profile.email != normalizedEmail) {
                        val syncedProfile = profile.copy(email = normalizedEmail)
                        updateUserProfile(syncedProfile)
                        trySend(syncedProfile)
                    } else {
                        trySend(profile)
                    }
                } else {
                    // Create default if not exists
                    val defaultProfile = UserProfile(
                        uid = uid,
                        email = email?.trim()?.lowercase().orEmpty()
                    )
                    updateUserProfile(defaultProfile)
                    trySend(defaultProfile)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    fun updateUserProfile(profile: UserProfile) {
        val userRef = database.child("users").child(profile.uid)
        val normalizedEmail = profile.email.trim().lowercase()
        userRef.setValue(profile.copy(email = normalizedEmail))
        if (normalizedEmail.isNotBlank()) {
            database.child("emailIndex").child(encodeEmail(normalizedEmail)).setValue(profile.uid)
        }
    }

    fun observeRideMeta(rideId: String): Flow<RideMeta?> = callbackFlow {
        val metaRef = database.child("rides").child(rideId).child("meta")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(RideMeta::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        metaRef.addValueEventListener(listener)
        awaitClose { metaRef.removeEventListener(listener) }
    }
}

data class InviteResolutionResult(
    val resolvedUids: List<String>,
    val missingEmails: List<String>
)

sealed class JoinRideCheck {
    object NotFound : JoinRideCheck()
    object NotInvited : JoinRideCheck()
    object Invited : JoinRideCheck()
    object AlreadyMember : JoinRideCheck()
}
