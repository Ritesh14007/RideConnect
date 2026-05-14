package com.rideconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground
import com.rideconnect.app.viewmodel.RideActionState

@Composable
fun StartRideScreen(
    rideActionState: RideActionState,
    currentLocation: com.google.android.gms.maps.model.LatLng? = null,
    onStart: (rideName: String, destination: String, destLat: Double, destLng: Double, invitedEmails: List<String>, waypoints: List<com.rideconnect.app.models.Waypoint>, isPublic: Boolean) -> Unit,
    onNavigateToLocationSearch: (target: String) -> Unit = {},
    onNavigateToMap: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var rideName by rememberSaveable { mutableStateOf("") }
    var destination by rememberSaveable { mutableStateOf("") }
    var destLat by rememberSaveable { mutableDoubleStateOf(0.0) }
    var destLng by rememberSaveable { mutableDoubleStateOf(0.0) }
    var newMemberInput by rememberSaveable { mutableStateOf("") }
    // Use remember (not rememberSaveable) — Waypoint is not Parcelable (Firebase needs plain data class)
    var invitedMembers by remember { mutableStateOf(emptyList<String>()) }
    var waypoints by remember { mutableStateOf(emptyList<com.rideconnect.app.models.Waypoint>()) }
    var newWaypointName by rememberSaveable { mutableStateOf("") }
    var isPublicRide by rememberSaveable { mutableStateOf(true) }

    // Observe result from location search
    val navBackStackEntry = (androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current as? androidx.navigation.NavBackStackEntry)
    val selectedLocation = navBackStackEntry?.savedStateHandle?.get<String>("selected_location")
    val selectedLat = navBackStackEntry?.savedStateHandle?.get<Double>("selected_lat") ?: 0.0
    val selectedLng = navBackStackEntry?.savedStateHandle?.get<Double>("selected_lng") ?: 0.0
    val locationTarget = navBackStackEntry?.savedStateHandle?.get<String>("location_target")
    
    LaunchedEffect(selectedLocation) {
        if (locationTarget == "destination") {
            selectedLocation?.let { 
                destination = it 
                destLat = selectedLat
                destLng = selectedLng
            }
        } else if (locationTarget?.startsWith("waypoint_") == true) {
            val name = navBackStackEntry?.savedStateHandle?.get<String>("waypoint_name") ?: "Stop"
            selectedLocation?.let {
                waypoints = waypoints + com.rideconnect.app.models.Waypoint(
                    id = "WP_${System.currentTimeMillis()}",
                    name = name,
                    address = it,
                    lat = selectedLat,
                    lng = selectedLng
                )
                // Clear the state handle so it doesn't trigger again on rotation
                navBackStackEntry?.savedStateHandle?.remove<String>("selected_location")
                navBackStackEntry?.savedStateHandle?.remove<String>("location_target")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("MISSION BRIEF", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("START NEW RIDE", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
        
        Spacer(modifier = Modifier.height(32.dp))

        // MAP PREVIEW AREA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
        ) {
            // Placeholder map graphics can go here
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
            
            // CURRENT LOCATION BADGE
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(1.dp, BrightNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrightNeonGreen))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "STARTING FROM CURRENT LOCATION",
                    color = BrightNeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // RIDE NAME
        Text("RIDE NAME", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = rideName,
            onValueChange = { rideName = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("", color = Color.DarkGray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DESTINATION
        Text("DESTINATION", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = destination,
            onValueChange = { /* Read-only via search */ },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToLocationSearch("destination") },
            placeholder = { Text("Search destination...", color = Color.DarkGray) },
            trailingIcon = { Icon(Icons.Default.Search, "Search", tint = Color.DarkGray) },
            enabled = false, // Disable typing, force click
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF1E1E1E),
                disabledTextColor = Color.White,
                disabledIndicatorColor = Color.Transparent,
                disabledPlaceholderColor = Color.DarkGray
            ),
            shape = RoundedCornerShape(8.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // WAYPOINTS
        Text("WAYPOINTS (STOPS)", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newWaypointName,
                onValueChange = { newWaypointName = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("e.g. Fuel Stop, Coffee", color = Color.DarkGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1E1E1E),
                    unfocusedContainerColor = Color(0xFF1E1E1E),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = {
                    if (newWaypointName.isNotBlank()) {
                        navBackStackEntry?.savedStateHandle?.set("waypoint_name", newWaypointName)
                        onNavigateToLocationSearch("waypoint_${System.currentTimeMillis()}")
                        newWaypointName = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = BrightNeonGreen)
            }
        }
        
        if (waypoints.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                waypoints.forEachIndexed { index, wp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF161616))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BrightNeonGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wp.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(wp.address, color = Color.Gray, fontSize = 10.sp, maxLines = 1)
                        }
                        IconButton(onClick = { waypoints = waypoints - wp }) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PACE & PRIVACY ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161616))
                    .padding(16.dp)
            ) {
                val distanceKm = if (currentLocation != null && destLat != 0.0) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        currentLocation.latitude, currentLocation.longitude,
                        destLat, destLng, results
                    )
                    results[0] / 1000f
                } else null

                Column {
                    Icon(Icons.Default.LocationOn, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("DISTANCE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(if (distanceKm != null) "%.1f KM".format(distanceKm) else "-- KM", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161616))
                    .padding(16.dp)
            ) {
                Column {
                    Icon(Icons.Default.Visibility, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PRIVACY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(if (isPublicRide) "Public Ride" else "Invite Only", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PRIVACY TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161616))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("RIDE VISIBILITY", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(if (isPublicRide) "Anyone with Ride ID can join" else "Only invited members can join", color = Color.White, fontSize = 12.sp)
            }
            Switch(
                checked = isPublicRide,
                onCheckedChange = { isPublicRide = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BrightNeonGreen,
                    checkedTrackColor = BrightNeonGreen.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // SUCCESS STATE - SHOW RIDE ID
        if (rideActionState is RideActionState.Success) {
            val rideId = rideActionState.rideId
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1A2E1A))
                    .border(2.dp, BrightNeonGreen, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = BrightNeonGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "RIDE CREATED SUCCESSFULLY!",
                        color = BrightNeonGreen,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Share this Ride ID with your squad:",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Ride ID Display Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(2.dp, BrightNeonGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(vertical = 20.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            rideId,
                            color = BrightNeonGreen,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Copy Button
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(rideId))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, tint = BrightNeonGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COPY", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Share Button
                        Button(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Join my RideConnect ride!")
                                    putExtra(Intent.EXTRA_TEXT, "Join my ride on RideConnect! Ride ID: $rideId")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Ride ID"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SHARE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Go to Map Button
                    Button(
                        onClick = { onNavigateToMap() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("GO TO LIVE MAP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
        // START BUTTON
        Button(
            onClick = { onStart(rideName, destination, destLat, destLng, invitedMembers, waypoints, isPublicRide) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
            shape = RoundedCornerShape(16.dp),
            enabled = rideActionState !is RideActionState.Loading && rideName.isNotBlank() && destination.isNotBlank()
        ) {
            if (rideActionState is RideActionState.Loading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    "BECOME RIDE LEADER & START",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ElectricBolt, null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "BY STARTING THIS RIDE, YOU AGREE TO BE THE PRIMARY NAVIGATOR AND SAFETY LEAD FOR ALL JOINING MEMBERS.",
            color = Color.DarkGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        }
        Spacer(modifier = Modifier.height(48.dp))
    }
}
