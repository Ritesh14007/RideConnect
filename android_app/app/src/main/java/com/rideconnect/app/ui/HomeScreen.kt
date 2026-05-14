package com.rideconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground
import com.rideconnect.app.ui.theme.ErrorRed
import com.rideconnect.app.ui.theme.SurfaceColor
import com.rideconnect.app.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rideconnect.app.viewmodel.ProfileViewModel
import com.rideconnect.app.viewmodel.RideViewModel

@Composable
fun HomeScreen(
    rideViewModel: RideViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToStart: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToMap: () -> Unit,
    onSos: () -> Unit
) {
    val speed by rideViewModel.currentSpeedKmh.collectAsState()
    val distance by rideViewModel.totalDistanceKm.collectAsState()
    val currentRideId by rideViewModel.currentRideId.collectAsState()
    val hasActiveRide = !currentRideId.isNullOrBlank()
    val pendingInvites by rideViewModel.pendingInvites.collectAsState()
    
    val formattedSpeed = String.format("%.1f", speed)
    val formattedDistance = String.format("%.2f", distance)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "RIDECONNECT",
                    color = BrightNeonGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val profile by profileViewModel.userProfile.collectAsState()
        val displayName = profile?.displayName ?: "Rider"
        val totalDistance = profile?.totalDistanceKm ?: 0.0
        val isMetric = profile?.isMetric ?: true
        val distMult = if(isMetric) 1.0 else 0.621371
        val distLabel = if(isMetric) "KM" else "MI"
        val category = profile?.category ?: "AMATEUR"
    val activeRideLabel = currentRideId ?: "No active ride"
        
        Text("WELCOME BACK,", color = Color.Gray, fontSize = 12.sp, letterSpacing = 1.sp)
        Text(displayName, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrightNeonGreen))
            Spacer(modifier = Modifier.width(8.dp))
            Text("$category CATEGORY • ${String.format("%.1f", totalDistance * distMult)} $distLabel", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ACTIVE RIDE CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF161616))
        ) {
            // Background Map Pattern Simulation
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A1A)))
            
            // Badge
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Group, null, tint = BrightNeonGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (hasActiveRide) "LIVE TRACKING" else "READY", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Info
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(if (hasActiveRide) "ACTIVE RIDE" else "NO ACTIVE RIDE", color = BrightNeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(activeRideLabel, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(if (hasActiveRide) "Tracking squad members in real time" else "Start or join a ride to begin live tracking", color = Color.Gray, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PENDING INVITES
        if (pendingInvites.isNotEmpty()) {
            Text("PENDING INVITES", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            pendingInvites.forEach { invite ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2A2A))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(invite.rideName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("From: ${invite.ownerUid}", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { 
                                    rideViewModel.acceptInvite(invite) { onNavigateToMap() }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("ACCEPT", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { rideViewModel.declineInvite(invite) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("DECLINE")
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // START RIDE BIG CARD
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(BrightNeonGreen)
                .clickable { onNavigateToStart() }
                .padding(24.dp)
        ) {
            // Large Lightning Bolt Watermark
            Icon(
                imageVector = Icons.Default.ElectricBolt,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(120.dp).align(Alignment.CenterEnd).offset(x = 20.dp, y = (-20).dp)
            )
            
            Column {
                Icon(Icons.Default.DirectionsBike, "Start Ride", tint = Color.White, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("START RIDE", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("LAUNCH KINETIC TRACKING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // JOIN RIDE & RIDERS MAP
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Join Ride
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E1E))
                    .clickable { onNavigateToJoin() }
                    .padding(20.dp)
            ) {
                Column {
                    Icon(Icons.Default.GroupAdd, "Join Ride", tint = BrightNeonGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Join Ride", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Enter a secure ride ID from your squad leader", color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            // Riders Map
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E1E))
                    .clickable { onNavigateToMap() }
                    .padding(20.dp)
            ) {
                Column {
                    Icon(Icons.Default.Explore, "Map", tint = Color(0xFF67E8F9), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Riders Map", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(if (hasActiveRide) "Live telemetry of your active ride" else "Map becomes useful once a ride is active", color = Color.Gray, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SOS BUTTON
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (hasActiveRide) Color(0xFF1E1515) else Color(0xFF141414))
                .border(1.dp, if (hasActiveRide) Color(0xFF4A2525) else Color(0xFF2A2A2A), RoundedCornerShape(16.dp))
                .clickable(enabled = hasActiveRide) { onSos() }
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.Warning,
                    null,
                    tint = if (hasActiveRide) ErrorRed else Color.Gray,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "SOS",
                        color = if (hasActiveRide) ErrorRed else Color.Gray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (hasActiveRide) "EMERGENCY BROADCAST" else "START OR JOIN A RIDE TO ENABLE SOS",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Icon(Icons.Default.ChevronRight, null, tint = if (hasActiveRide) ErrorRed else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // BOTTOM STATS SCROLL ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("ELEV GAIN", "0.0", "M")
            StatCard("CURRENT PACE", formattedSpeed, "KM/H")
            StatCard("DISTANCE", formattedDistance, "KM")
        }
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun StatCard(title: String, value: String, unit: String) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = BrightNeonGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 3.dp))
            }
        }
    }
}
