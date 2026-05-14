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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground

import com.rideconnect.app.viewmodel.ProfileViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onNavigateToSettings: () -> Unit
) {
    val userProfile by profileViewModel.userProfile.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.Menu, "Menu", tint = BrightNeonGreen)
            Text(
                text = "RIDECONNECT",
                color = BrightNeonGreen,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            // Settings Icon
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.Gray,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onNavigateToSettings() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // AVATAR
        val profile = userProfile
        val displayName = profile?.displayName ?: "Rider"
        val category = profile?.category ?: "AMATEUR"
        val memberStatus = if (profile?.email.isNullOrBlank()) "Profile incomplete" else "Verified account"

        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E1E1E))
                    .border(2.dp, Color(0xFF333333), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Default.DirectionsBike, null, tint = BrightNeonGreen, modifier = Modifier.size(60.dp).align(Alignment.Center))
            }
            Box(
                modifier = Modifier
                    .offset(x = 10.dp, y = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrightNeonGreen)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(category, color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(displayName, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(memberStatus, color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("•", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("$category CATEGORY", color = BrightNeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // STATS BLOCKS
        val totalDist = profile?.totalDistanceKm ?: 0.0
        val elevGain = profile?.monthlyElevationGain ?: 0.0
        val avgSpeed = profile?.averageSpeedKmh ?: 0.0
        val isMetric = profile?.isMetric ?: true
        
        val distLabel = if (isMetric) "KM" else "MI"
        val distMult = if (isMetric) 1.0 else 0.621371
        
        ProfileStatBlock("TOTAL DISTANCE", String.format("%.1f", totalDist * distMult), distLabel, fraction = 0.8f)
        Spacer(modifier = Modifier.height(16.dp))
        ProfileStatBlock("ELEV GAIN", String.format("%.1f", elevGain * distMult), distLabel, fraction = 0.4f)
        Spacer(modifier = Modifier.height(16.dp))
        ProfileStatBlock("AVG SPEED", String.format("%.1f", avgSpeed * distMult), if(isMetric) "KM/H" else "MPH", fraction = 0.6f)

        Spacer(modifier = Modifier.height(32.dp))

        // MONTHLY PROGRESS summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Ride Summary", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("PROFILE DATA", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF161616))
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                ProfileSummaryRow("Email", profile?.email?.ifBlank { "Not provided" } ?: "Not provided")
                ProfileSummaryRow("Auto-pause", if (profile?.autoPauseEnabled != false) "Enabled" else "Disabled")
                ProfileSummaryRow("Units", if (isMetric) "Metric" else "Imperial")
                ProfileSummaryRow("Group messages", if (profile?.groupMessagesEnabled != false) "Enabled" else "Disabled")
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // CURRENT STATUS
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Current Status", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AchievementCard(if (totalDist > 0.0) "ACTIVE RIDER" else "NEW RIDER", Icons.Default.WorkspacePremium, Modifier.weight(1f))
            AchievementCard(if (avgSpeed > 0.0) "PACE SET" else "PACE TBD", Icons.Default.Terrain, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        // GEAR SETUP
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Gear Setup", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        val gearTitle = profile?.gearTitle ?: "Standard Road Bike"
        val gearSubtitle = profile?.gearSubtitle ?: "Aluminum Frame"
        GearItem(gearTitle, gearSubtitle)
        
        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
fun ProfileSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End)
    }
}

@Composable
fun ProfileStatBlock(title: String, value: String, unit: String, fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text(unit, color = BrightNeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).background(Color(0xFF222222))) {
                Box(modifier = Modifier.fillMaxWidth(fraction).height(4.dp).background(BrightNeonGreen))
            }
        }
    }
}

@Composable
fun AchievementCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(60.dp).border(2.dp, BrightNeonGreen, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun GearItem(title: String, subtitle: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF161616))
            .clickable { 
                android.widget.Toast.makeText(context, "Gear details for $title to be implemented", android.widget.Toast.LENGTH_SHORT).show()
            }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF222222)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Build, null, tint = BrightNeonGreen, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
        }
    }
}
