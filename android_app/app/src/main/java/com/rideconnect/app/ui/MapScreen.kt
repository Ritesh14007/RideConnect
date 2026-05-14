package com.rideconnect.app.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.rideconnect.app.R
import com.rideconnect.app.models.Rider
import com.rideconnect.app.ui.theme.*
import com.rideconnect.app.viewmodel.FollowMode
import com.rideconnect.app.viewmodel.RideViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

// Distinct colors per rider slot
private val riderPalette = listOf(
    Color(0xFF7C4DFF), // purple
    Color(0xFF00BCD4), // teal
    Color(0xFFFF6D00), // orange
    Color(0xFFE91E63), // pink
    Color(0xFF43A047), // green
    Color(0xFFFDD835)  // yellow
)

// Day Mode Style (Standard with neon accents)
private val DayMapStyle = "[" +
    "  { \"featureType\": \"water\", \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#c9c9c9\" } ] }," +
    "  { \"featureType\": \"landscape\", \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#f5f5f5\" } ] }" +
    "]"

// Night Mode Style (Pure black/neon)
private val NightMapStyle = "[" +
    "  { \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#000000\" } ] }," +
    "  { \"elementType\": \"labels.text.stroke\", \"stylers\": [ { \"color\": \"#000000\" } ] }," +
    "  { \"elementType\": \"labels.text.fill\", \"stylers\": [ { \"color\": \"#39FF14\" }, { \"weight\": 0.1 } ] }," +
    "  { \"featureType\": \"road\", \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#1A1A1A\" } ] }," +
    "  { \"featureType\": \"road.highway\", \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#222222\" } ] }," +
    "  { \"featureType\": \"water\", \"elementType\": \"geometry\", \"stylers\": [ { \"color\": \"#000814\" } ] }" +
    "]"

private fun initials(name: String): String {
    val parts = name.trim().split(" ")
    return if (parts.size >= 2) "${parts[0].first()}${parts[1].first()}".uppercase()
    else name.take(2).uppercase()
}

// Determine rider's live status from Firebase data
private fun riderStatus(rider: Rider): String {
    val nowMs = System.currentTimeMillis()
    val staleMs = 60_000L
    return when {
        rider.isSos                         -> "SOS"
        nowMs - rider.lastUpdated > staleMs -> "OFFLINE"
        rider.speedKmh > 1f                 -> "MOVING"
        else                                -> "STOPPED"
    }
}

private fun statusColor(status: String) = when (status) {
    "MOVING"  -> BrightNeonGreen
    "STOPPED" -> Color(0xFFFDD835)
    "SOS"     -> Color(0xFFFF3B30)
    else      -> Color(0xFF555555)
}

private fun statusBg(status: String) = when (status) {
    "MOVING"  -> Color(0xFF0D2B14)
    "STOPPED" -> Color(0xFF2B2700)
    "SOS"     -> Color(0xFF2B0A08)
    else      -> Color(0xFF1A1A1A)
}

private fun launchDialer(context: Context, rider: Rider) {
    val phoneNumber = rider.phoneNumber.trim()
    if (phoneNumber.isBlank()) {
        Toast.makeText(context, "${rider.name} has not added a phone number", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phoneNumber)}"))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open phone dialer", Toast.LENGTH_SHORT).show()
    }
}


private fun formatDuration(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun calculateDistance(loc1: LatLng?, loc2: LatLng): Float {
    if (loc1 == null) return 0f
    val results = FloatArray(1)
    android.location.Location.distanceBetween(loc1.latitude, loc1.longitude, loc2.latitude, loc2.longitude, results)
    return results[0] / 1000f // to km
}

private fun createMarkerIcon(color: Color, isSos: Boolean): BitmapDescriptor {
    val size = if (isSos) 80 else 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val paint = Paint().apply {
        this.color = color.toArgb()
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)
    
    paint.apply {
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = if (isSos) 6f else 4f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, paint)
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/** Fetch a route polyline from Google Directions API */
private suspend fun fetchRoutePolyline(
    origin: LatLng,
    destLat: Double,
    destLng: Double,
    waypoints: List<com.rideconnect.app.models.Waypoint>,
    apiKey: String
): List<LatLng> = withContext(Dispatchers.IO) {
    try {
        val waypointsParam = if (waypoints.isNotEmpty()) {
            "&waypoints=optimize:true|via:" + waypoints.joinToString("|via:") { 
                String.format(java.util.Locale.US, "%.6f,%.6f", it.lat, it.lng) 
            }
        } else ""
        
        val originStr = String.format(java.util.Locale.US, "%.6f,%.6f", origin.latitude, origin.longitude)
        val destStr = String.format(java.util.Locale.US, "%.6f,%.6f", destLat, destLng)
        
        val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=$originStr" +
                "&destination=$destStr" +
                waypointsParam +
                "&mode=driving" +
                "&avoid=ferries" +
                "&key=$apiKey"
        
        val response = URL(urlString).readText()
        val json = JSONObject(response)
        
        if (json.getString("status") != "OK") {
            return@withContext emptyList()
        }
        
        val routes = json.getJSONArray("routes")
        if (routes.length() == 0) return@withContext emptyList()
        
        val points = routes.getJSONObject(0)
            .getJSONObject("overview_polyline")
            .getString("points")
        
        val decoded = decodePolyline(points)
        
        // Only return if we have at least 2 points to avoid weird single-point lines
        if (decoded.size < 2) return@withContext emptyList()
        
        return@withContext decoded
    } catch (e: Exception) {
        emptyList()
    }
}

/** Decode Google's encoded polyline format */
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        poly.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return poly
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: RideViewModel,
    onNavigateToChat: (String) -> Unit = {}
) {
    val riders         by viewModel.riders.collectAsState()
    val rideInvites    by viewModel.rideInvites.collectAsState()
    val rideInviteLabels by viewModel.rideInviteLabels.collectAsState()
    val currentRideId  by viewModel.currentRideId.collectAsState()
    val waypoints      by viewModel.currentWaypoints.collectAsState()
    val destination    by viewModel.rideDestination.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val rideDurationMillis by viewModel.rideDurationMillis.collectAsState()
    val followMode     by viewModel.followMode.collectAsState()
    val isDayMode      by viewModel.isDayMode.collectAsState()
    val isOnline       by viewModel.isOnline.collectAsState()
    val destLat        by viewModel.destLat.collectAsState()
    val destLng        by viewModel.destLng.collectAsState()
    val distToDest     by viewModel.distanceToDestinationKm.collectAsState()
    val timeToDest     by viewModel.timeToDestMinutes.collectAsState()
    val context        = LocalContext.current

    // Route polyline state
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var lastRouteFetchLocation by remember { mutableStateOf<LatLng?>(null) }
    val apiKey = context.getString(R.string.google_maps_key)

    // Fetch route whenever we have a destination and current location
    LaunchedEffect(currentLocation, destLat, destLng, waypoints) {
        val loc = currentLocation
        if (loc != null && destLat != 0.0 && destLng != 0.0) {
            // Only re-fetch if we've moved more than 50 meters or if we don't have a route yet
            val distMoved = lastRouteFetchLocation?.let { 
                calculateDistance(it, loc) * 1000f // to meters
            } ?: 100f
            
            if (routePoints.isEmpty() || distMoved > 50f) {
                routePoints = fetchRoutePolyline(loc, destLat, destLng, waypoints, apiKey)
                lastRouteFetchLocation = loc
            }
        }
    }

    val defaultLatLng = remember { LatLng(20.5937, 78.9629) } // Center of India
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLocation ?: defaultLatLng, 15f)
    }

    // Handle Follow Mode logic
    LaunchedEffect(currentLocation, riders, followMode) {
        val targetLoc = when (followMode) {
            FollowMode.FOLLOW_ME -> currentLocation
            FollowMode.FOLLOW_LEADER -> riders.firstOrNull()?.let { LatLng(it.lat, it.lng) }
        }
        if (targetLoc != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(targetLoc, 16f),
                1000
            )
        }
    }

    val onlineCount      = riders.count { riderStatus(it) != "OFFLINE" }
    val totalCount       = riders.size + rideInvites.size
    var showEndRideDialog by remember { mutableStateOf(false) }
    var showSosConfirmation by remember { mutableStateOf(false) }
    var selectedRider by remember { mutableStateOf<Rider?>(null) }

    val scaffoldState = rememberBottomSheetScaffoldState()
    
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 100.dp,
        sheetContainerColor = SurfaceColor,
        sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        sheetContent = {
            RiderStatusPanel(
                modifier = Modifier.fillMaxHeight(0.6f),
                riders = riders,
                invites = rideInvites,
                inviteLabels = rideInviteLabels,
                myLoc = currentLocation,
                myId = viewModel.riderId,
                onCall = { rider ->
                    selectedRider = rider
                    launchDialer(context, rider)
                },
                onMessage = { /* message */ }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(DarkBackground).padding(paddingValues)) {

        // ── MAP ──────────────────────────────────────────────────────────────
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isTrafficEnabled = false,
                mapStyleOptions = MapStyleOptions(if (isDayMode) DayMapStyle else NightMapStyle)
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false
            )
        ) {
            // ── ROUTE POLYLINE ─────────────────────────────────────────────
            if (routePoints.isNotEmpty()) {
                Polyline(
                    points = routePoints,
                    color = BrightNeonGreen.copy(alpha = 0.2f),
                    width = 25f
                )
                Polyline(
                    points = routePoints,
                    color = BrightNeonGreen,
                    width = 8f
                )
            }

            // ── RIDER MARKERS ──────────────────────────────────────────────
            riders.forEach { rider ->
                val status = riderStatus(rider)
                val color  = statusColor(status)
                val isMe   = rider.id == viewModel.riderId
                Marker(
                    state = rememberMarkerState(position = LatLng(rider.lat, rider.lng)),
                    title = if (isMe) "YOU" else rider.name,
                    snippet = status,
                    icon  = createMarkerIcon(if (isMe) NeonBlue else color, rider.isSos),
                    onClick = {
                        selectedRider = rider
                        false
                    }
                )
            }
        }

        // ── TOP BAR (Ride Info) ──────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrightNeonGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrightNeonGreen))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ACTIVE", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                    Text(
                        if (timeToDest != null) {
                            val h = timeToDest!! / 60
                            val m = timeToDest!! % 60
                            if (h > 0) "${h}h ${m}m left" else "${m}m left"
                        } else {
                            formatDuration(rideDurationMillis)
                        },
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        if (timeToDest != null) "TO DESTINATION" else "RIDE DURATION",
                        color = BrightNeonGreen, 
                        fontSize = 10.sp, 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.sp
                    )
                    distToDest?.let {
                        Text("%.1f km remaining".format(it), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // END RIDE BUTTON
                IconButton(
                    onClick = { showEndRideDialog = true },
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "End Ride", tint = Color.Red, modifier = Modifier.size(24.dp))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.People, contentDescription = null, tint = BrightNeonGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$onlineCount/$totalCount Riders", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleDayMode() }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (isDayMode) Icons.Default.Nightlight else Icons.Default.LightMode,
                                contentDescription = "Toggle Theme",
                                tint = if (isDayMode) Color.Gray else BrightNeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isOnline) NeonBlue else Color.Red,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ── FLOATING ACTION BUTTONS (Right Side) ────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActionButton(Icons.Default.Chat, BrightNeonGreen) { currentRideId?.let(onNavigateToChat) }
            ActionButton(Icons.Default.Call, NeonBlue) {
                val riderToCall = selectedRider?.takeIf { it.id != viewModel.riderId }
                    ?: riders.firstOrNull { it.id != viewModel.riderId }
                when {
                    currentRideId == null -> Toast.makeText(context, "Start or join a ride first", Toast.LENGTH_SHORT).show()
                    riderToCall == null -> Toast.makeText(context, "No other rider to call yet", Toast.LENGTH_SHORT).show()
                    else -> launchDialer(context, riderToCall)
                }
            }
            ActionButton(Icons.Default.Warning, Color.Red) { showSosConfirmation = true }
        }

        // ── FOLLOW MODE & QUICK ACTIONS ─────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 116.dp) // Above the 100dp peek height
                .fillMaxWidth()
        ) {
            // Follow Mode Toggle
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FollowModeTab("Follow Me", followMode == FollowMode.FOLLOW_ME) { viewModel.setFollowMode(FollowMode.FOLLOW_ME) }
                FollowModeTab("Follow Leader", followMode == FollowMode.FOLLOW_LEADER) { viewModel.setFollowMode(FollowMode.FOLLOW_LEADER) }
            }


        }

        // ── DIALOGS ───────────────────────────────────────────────────────
        if (showSosConfirmation) {
            AlertDialog(
                onDismissRequest = { showSosConfirmation = false },
                title = { Text("EMERGENCY SOS", color = Color.Red, fontWeight = FontWeight.Black) },
                text = { Text("This will send your live location and an emergency alert to all riders in the group. Proceed?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.triggerSos()
                            showSosConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("SEND SOS", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSosConfirmation = false }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                containerColor = SurfaceColor,
                titleContentColor = Color.White,
                textContentColor = Color.LightGray
            )
        }

        if (showEndRideDialog) {
            AlertDialog(
                onDismissRequest = { showEndRideDialog = false },
                title = { Text("END RIDE?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("Are you sure you want to end this ride? Tracking will stop for you.", color = Color.LightGray) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.leaveRide()
                            showEndRideDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("END MISSION", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEndRideDialog = false }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                containerColor = SurfaceColor
            )
        }
        }
    }
}

@Composable
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = color,
        contentColor = Color.Black,
        shape = CircleShape,
        modifier = Modifier.size(56.dp) // Reduced from 64dp
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp)) // Reduced from 32dp
    }
}

@Composable
fun FollowModeTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(if (isSelected) BrightNeonGreen else Color.Transparent)
    val textColor by animateColorAsState(if (isSelected) Color.Black else Color.Gray)
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}


@Composable
fun RiderStatusPanel(
    modifier: Modifier,
    riders: List<Rider>,
    invites: List<String>,
    inviteLabels: Map<String, String>,
    myLoc: LatLng?,
    myId: String,
    onCall: (Rider) -> Unit,
    onMessage: (Rider) -> Unit
) {
    Column(modifier = modifier.padding(horizontal = 24.dp).padding(top = 8.dp)) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF333333))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text("RIDER STATUS", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(riders) { rider ->
                RiderRow(rider, myLoc, rider.id == myId, onCall, onMessage)
            }
        }
    }
}

@Composable
fun RiderRow(rider: Rider, myLoc: LatLng?, isMe: Boolean, onCall: (Rider) -> Unit, onMessage: (Rider) -> Unit) {
    val status = riderStatus(rider)
    val distance = calculateDistance(myLoc, LatLng(rider.lat, rider.lng))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isMe) Color(0xFF1A1A1A) else Color(0xFF111111))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background((if (isMe) NeonBlue else statusColor(status)).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(rider.name), color = if (isMe) NeonBlue else statusColor(status), fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (isMe) "YOU (${rider.name})" else rider.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor(status)))
                Spacer(modifier = Modifier.width(6.dp))
                Text(status, color = statusColor(status), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(12.dp))
                Text("${rider.speedKmh.toInt()} km/h", color = Color.Gray, fontSize = 11.sp)
                if (!isMe) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("•", color = Color.DarkGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("%.1f km away".format(distance), color = Color.Gray, fontSize = 11.sp)
                }
            }
        }
        if (!isMe) {
            Row {
                IconButton(onClick = { onCall(rider) }) { Icon(Icons.Default.Call, contentDescription = null, tint = NeonBlue) }
                IconButton(onClick = { onMessage(rider) }) { Icon(Icons.Default.Chat, contentDescription = null, tint = BrightNeonGreen) }
            }
        }
    }
}
