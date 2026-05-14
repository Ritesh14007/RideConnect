package com.rideconnect.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun JoinRideScreen(
    rideActionState: RideActionState,
    onJoin: (String) -> Unit,
    onNavigateToMap: () -> Unit = {}
) {
    var rideId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // SUCCESS STATE
        if (rideActionState is RideActionState.Success) {
            val joinedRideId = (rideActionState as RideActionState.Success).rideId
            
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = BrightNeonGreen,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "JOINED SUCCESSFULLY!",
                color = BrightNeonGreen,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "You are now connected to ride:",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // Ride ID Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A2E1A))
                    .border(2.dp, BrightNeonGreen, RoundedCornerShape(16.dp))
                    .padding(horizontal = 32.dp, vertical = 16.dp)
            ) {
                Text(
                    joinedRideId,
                    color = BrightNeonGreen,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onNavigateToMap,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "GO TO LIVE MAP",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            
            return@Column
        }
        
        // NORMAL JOIN STATE
        Icon(Icons.Default.GroupAdd, contentDescription = "Join Ride", tint = BrightNeonGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("JOIN RIDE", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Text("ENTER THE RIDE ID FROM YOUR SQUAD LEADER", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)

        Spacer(modifier = Modifier.height(48.dp))
        
        // Instructions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E1E1E))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    "HOW TO JOIN:",
                    color = BrightNeonGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Ask your ride leader for the Ride ID\n2. Enter the 6-character code below\n3. Tap JOIN RIDE to connect",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = rideId,
            onValueChange = { 
                // Convert to uppercase and limit length
                rideId = it.uppercase().take(20)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("ENTER RIDE ID (e.g., AB12CD)", color = Color.DarkGray) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1E1E1E),
                focusedIndicatorColor = BrightNeonGreen,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = BrightNeonGreen,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (rideActionState is RideActionState.Error) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2A1A1A))
                    .border(1.dp, Color(0xFFFF7351), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = rideActionState.message,
                    color = Color(0xFFFF7351),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (rideId.isNotBlank()) {
                    onJoin(rideId.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
            shape = RoundedCornerShape(16.dp),
            enabled = rideId.isNotBlank() && rideActionState !is RideActionState.Loading
        ) {
            if (rideActionState is RideActionState.Loading) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    "JOIN RIDE",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Public rides can be joined by anyone with the Ride ID",
            color = Color.DarkGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}
