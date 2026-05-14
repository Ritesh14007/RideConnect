package com.rideconnect.app.ui

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rideconnect.app.R
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground
import com.rideconnect.app.viewmodel.ProfileViewModel

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoScreen(
    profileViewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToLocationSearch: () -> Unit = {}
) {
    val profile by profileViewModel.userProfile.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var dob by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var riderId by remember { mutableStateOf("") }
    var profilePictureUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        profilePictureUri = uri
    }

    // Observe result from location search
    val navBackStackEntry = (androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current as? androidx.navigation.NavBackStackEntry)
    val selectedLocation = navBackStackEntry?.savedStateHandle?.get<String>("selected_location")
    val locationTarget = navBackStackEntry?.savedStateHandle?.get<String>("location_target")
    
    LaunchedEffect(selectedLocation) {
        if (locationTarget == "profile") {
            selectedLocation?.let { 
                location = it 
                navBackStackEntry?.savedStateHandle?.remove<String>("selected_location")
            }
        }
    }

    var showGenderMenu by remember { mutableStateOf(false) }
    var showBloodMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val datePickerState = rememberDatePickerState(
        initialDisplayMode = DisplayMode.Picker
    )

    // Only initialize once when profile is first loaded
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(profile) {
        if (profile != null && !isInitialized) {
            fullName = profile!!.displayName
            email = profile!!.email
            phoneNumber = profile!!.phoneNumber
            gender = profile!!.gender
            bloodGroup = profile!!.bloodGroup
            dob = profile!!.dob
            location = profile!!.location
            riderId = profile!!.riderId
            if (profile!!.profilePictureUrl.isNotBlank()) {
                profilePictureUri = android.net.Uri.parse(profile!!.profilePictureUrl)
            }
            isInitialized = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // TOP APP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = BrightNeonGreen,
                modifier = Modifier.clickable { onBack() }
            )
            Text(
                "PROFILE INFO",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = BrightNeonGreen,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.width(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture Section
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val painter = if (profilePictureUri != null) {
                    rememberAsyncImagePainter(profilePictureUri)
                } else {
                    painterResource(id = R.drawable.ic_rider_placeholder)
                }
                
                Image(
                    painter = painter,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, BrightNeonGreen, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BrightNeonGreen)
                        .border(2.dp, DarkBackground, CircleShape)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text("Tap to change photo", color = Color.Gray, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(32.dp))

            // PERSONAL INFORMATION
            ProfileSectionHeader("PERSONAL INFORMATION")
            
            ProfileTextField(Icons.Default.Person, "Rider Name", fullName) { fullName = it }
            ProfileTextField(Icons.Default.Email, "Email Address", email) { email = it }
            ProfileTextField(Icons.Default.Phone, "Phone Number", phoneNumber) { phoneNumber = it }
            
            // Gender Dropdown
            Box {
                ProfileDropdownField(Icons.Default.Wc, "Gender", gender) { showGenderMenu = true }
                DropdownMenu(
                    expanded = showGenderMenu,
                    onDismissRequest = { showGenderMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    listOf("Male", "Female", "Other").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White) },
                            onClick = {
                                gender = option
                                showGenderMenu = false
                            }
                        )
                    }
                }
            }

            // Blood Group Dropdown
            Box {
                ProfileDropdownField(Icons.Default.WaterDrop, "Blood Group", bloodGroup) { showBloodMenu = true }
                DropdownMenu(
                    expanded = showBloodMenu,
                    onDismissRequest = { showBloodMenu = false },
                    modifier = Modifier.background(Color(0xFF1E1E1E))
                ) {
                    listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = Color.White) },
                            onClick = {
                                bloodGroup = option
                                showBloodMenu = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ADDITIONAL INFORMATION
            ProfileSectionHeader("ADDITIONAL INFORMATION")
            
            ProfileDropdownField(Icons.Default.CalendarToday, "Date of Birth", dob) { showDatePicker = true }
            
            // Location field with smart search
            val navBackStackEntry = (androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current as? androidx.navigation.NavBackStackEntry)
            val selectedLocation = navBackStackEntry?.savedStateHandle?.get<String>("selected_location")
            LaunchedEffect(selectedLocation) {
                selectedLocation?.let { location = it }
            }
            
            ProfileDropdownField(Icons.Default.LocationOn, "Location", location) {
                // Navigate to search
                // Note: I need the nav controller here. I'll pass it or use a callback.
                // For now I'll assume it's handled in the parent or I'll add a callback.
                onNavigateToLocationSearch()
            }
            ProfileTextField(Icons.Default.Badge, "Rider ID (Optional)", riderId) { riderId = it }

            Spacer(modifier = Modifier.height(40.dp))

            // SAVE BUTTON
    if (isSaving) {
        CircularProgressIndicator(color = BrightNeonGreen)
    } else {
        Button(
            onClick = {
                val currentProfile = profile
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                
                coroutineScope.launch {
                    isSaving = true
                    try {
                        if (currentProfile != null) {
                            val updatedProfile = currentProfile.copy(
                                displayName = fullName,
                                email = email,
                                phoneNumber = phoneNumber,
                                gender = gender,
                                bloodGroup = bloodGroup,
                                dob = dob,
                                location = location,
                                riderId = riderId
                            )
                            profileViewModel.uploadAndSaveProfile(updatedProfile, profilePictureUri)
                            android.widget.Toast.makeText(context, "Profile Updated Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        } else if (currentUser != null) {
                            val newProfile = com.rideconnect.app.models.UserProfile(
                                uid = currentUser.uid,
                                email = currentUser.email ?: email,
                                displayName = fullName,
                                phoneNumber = phoneNumber,
                                gender = gender,
                                bloodGroup = bloodGroup,
                                dob = dob,
                                location = location,
                                riderId = riderId
                            )
                            profileViewModel.uploadAndSaveProfile(newProfile, profilePictureUri)
                            android.widget.Toast.makeText(context, "Profile Created Successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            onBack()
                        }
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Error saving: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrightNeonGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("SAVE CHANGES", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val date = java.util.Date(it)
                        val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                        dob = format.format(date)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = BrightNeonGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.White,
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = BrightNeonGreen,
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = BrightNeonGreen,
                dayContentColor = Color.White,
                disabledDayContentColor = Color.DarkGray,
                selectedDayContentColor = Color.Black,
                selectedDayContainerColor = BrightNeonGreen,
                todayContentColor = BrightNeonGreen
            )
        ) {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color.White,
                    onSurfaceVariant = Color.White,
                    primary = BrightNeonGreen,
                    onPrimary = Color.Black,
                    secondary = BrightNeonGreen
                )
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTextField(icon: ImageVector, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        label = { Text(label, color = Color.Gray, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, null, tint = BrightNeonGreen) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            focusedBorderColor = BrightNeonGreen,
            unfocusedBorderColor = Color.Transparent,
            containerColor = Color(0xFF131313),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = BrightNeonGreen
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun ProfileDropdownField(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF131313))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = BrightNeonGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = Color.Gray, fontSize = 10.sp)
                Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ExpandMore, null, tint = Color.Gray)
        }
    }
}
