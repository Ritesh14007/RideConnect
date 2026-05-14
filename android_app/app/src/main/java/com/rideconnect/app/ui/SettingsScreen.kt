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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground
import com.rideconnect.app.viewmodel.AuthViewModel
import com.rideconnect.app.viewmodel.ProfileViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onNavigateToProfileInfo: () -> Unit
) {
    val profile by profileViewModel.userProfile.collectAsState()
    val displayName = profile?.displayName ?: "Rider"
    val category = profile?.category ?: "AMATEUR"
    val email = profile?.email ?: "No email linked"
    val autoPause = profile?.autoPauseEnabled ?: true
    val groupMessages = profile?.groupMessagesEnabled ?: true
    val sosAlerts = profile?.sosAlertsEnabled ?: true
    val pushAlerts = profile?.pushAlertsEnabled ?: false
    val isMetric = profile?.isMetric ?: true
    

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(top = 16.dp),
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = BrightNeonGreen, modifier = Modifier.clickable { onBack() })
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", color = BrightNeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(24.dp))

            // PROFILE BLOCK
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.weight(1.5f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E)).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.Gray)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BrightNeonGreen).align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("$category rider", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                Box(
                    modifier = Modifier.weight(1f).height(80.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0F2611)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ElectricBolt, null, tint = BrightNeonGreen)
                        Text("SYNCED", color = BrightNeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(email, color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(32.dp))

            SettingsListItem(
                icon = Icons.Default.Person,
                title = "Profile Info",
                trailing = { Text(displayName, color = Color.Gray, fontSize = 12.sp) },
                onClick = {
                    onNavigateToProfileInfo()
                }
            )
            SettingsListItem(
                icon = Icons.Default.Lock,
                title = "Password Reset",
                trailing = { Text("Email login", color = Color.Gray, fontSize = 12.sp) },
                onClick = {
                    if (email != "No email linked") {
                        authViewModel.sendPasswordReset(email)
                    }
                }
            )
            SettingsListItem(
                icon = Icons.Default.Extension,
                title = "Linked Apps",
                trailing = { Text("Email only", color = Color.Gray, fontSize = 12.sp) }
            )

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeading("RIDE PREFERENCES")
            SettingsListItem(Icons.Default.PauseCircleFilled, "Auto-pause", trailing = {
                Switch(checked = autoPause, onCheckedChange = { profileViewModel.toggleAutoPause(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrightNeonGreen, uncheckedThumbColor = Color.LightGray))
            })
            SettingsListItem(Icons.Default.SquareFoot, "Units", trailing = {
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF222222))) {
                    Box(modifier = Modifier.background(if(isMetric) BrightNeonGreen else Color.Transparent, RoundedCornerShape(8.dp)).clickable { profileViewModel.toggleUnits(true) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("METRIC", color = if(isMetric) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.background(if(!isMetric) BrightNeonGreen else Color.Transparent, RoundedCornerShape(8.dp)).clickable { profileViewModel.toggleUnits(false) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("IMP", color = if(!isMetric) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            })
            SettingsListItem(Icons.Default.Map, "Map Style", trailing = {
                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF222222))) {
                    val isDark = profile?.mapStyleDarkMode != false
                    Box(modifier = Modifier.background(if(!isDark) BrightNeonGreen else Color.Transparent, RoundedCornerShape(8.dp)).clickable { profileViewModel.toggleMapStyle(false) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("LIGHT", color = if(!isDark) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.background(if(isDark) BrightNeonGreen else Color.Transparent, RoundedCornerShape(8.dp)).clickable { profileViewModel.toggleMapStyle(true) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("DARK", color = if(isDark) Color.Black else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            })

            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeading("NOTIFICATIONS")
            SettingsListItem(Icons.Default.Notifications, "Push Alerts", trailing = {
                Switch(checked = pushAlerts, onCheckedChange = { profileViewModel.togglePushAlerts(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrightNeonGreen, uncheckedThumbColor = Color.LightGray))
            })
            SettingsListItem(Icons.Default.ChatBubble, "Group Messages", trailing = {
                Switch(checked = groupMessages, onCheckedChange = { profileViewModel.toggleGroupMessages(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrightNeonGreen, uncheckedThumbColor = Color.LightGray))
            })
            SettingsListItem(Icons.Default.CellTower, "SOS Alerts", trailing = {
                Switch(checked = sosAlerts, onCheckedChange = { profileViewModel.toggleSosAlerts(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrightNeonGreen, uncheckedThumbColor = Color.LightGray))
            })

            Spacer(modifier = Modifier.height(32.dp))
            
            // LOG OUT
            Button(
                onClick = {
                    authViewModel.signOut()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("LOG OUT", color = Color(0xFFFF7351), fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("VERSION 4.2.0-KINETIC", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsSectionHeading(title: String) {
    Text(
        text = title,
        color = Color.LightGray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun SettingsListItem(icon: ImageVector, title: String, onClick: (() -> Unit)? = null, trailing: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            if (onClick != null) {
                onClick()
            } else {
                android.widget.Toast.makeText(context, "$title screen to be implemented", android.widget.Toast.LENGTH_SHORT).show()
            }
        }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}
