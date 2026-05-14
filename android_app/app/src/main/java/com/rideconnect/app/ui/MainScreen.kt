package com.rideconnect.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rideconnect.app.ui.theme.BrightNeonGreen
import com.rideconnect.app.ui.theme.DarkBackground
import com.rideconnect.app.ui.theme.SurfaceColor
import com.rideconnect.app.viewmodel.AuthViewModel
import com.rideconnect.app.viewmodel.RideActionState
import com.rideconnect.app.viewmodel.RideViewModel
import com.rideconnect.app.viewmodel.ProfileViewModel
import com.rideconnect.app.viewmodel.ChatViewModel
import com.rideconnect.app.ui.ChatScreen
import com.rideconnect.app.ui.HistoryScreen
import com.rideconnect.app.ui.LocationSearchScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

sealed class BottomNavItem(var title: String, var icon: ImageVector, var route: String) {
    object Home : BottomNavItem("HOME", Icons.Default.Home, "main_home")
    object Start : BottomNavItem("START", Icons.Default.DirectionsBike, "main_start")
    object Join : BottomNavItem("JOIN", Icons.Default.GroupAdd, "main_join")
    object Map : BottomNavItem("MAP", Icons.Default.Explore, "main_map")
    object History : BottomNavItem("HISTORY", Icons.Default.History, "main_history")
    object Profile : BottomNavItem("PROFILE", Icons.Default.Person, "main_profile")
}

@Composable
fun MainScreen(
    rideViewModel: RideViewModel,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel = viewModel(),
    chatViewModel: ChatViewModel = viewModel(),
    rootNavController: NavHostController // To go back to login if needed
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val rideActionState by rideViewModel.rideActionState.collectAsState()

    LaunchedEffect(rideActionState) {
        when (val state = rideActionState) {
            is RideActionState.Success -> {
                bottomNavController.navigate("main_map")
                rideViewModel.resetRideActionState()
            }
            is RideActionState.Error -> {
                if (currentRoute == BottomNavItem.Home.route) {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    rideViewModel.resetRideActionState()
                }
            }
            else -> Unit
        }
    }

    // Define items
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Start,
        BottomNavItem.Join,
        BottomNavItem.Map,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrightNeonGreen,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = BrightNeonGreen,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        ),
                        onClick = {
                            if (currentRoute != item.route) {
                                bottomNavController.navigate(item.route) {
                                    popUpTo(bottomNavController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(paddingValues).padding(bottom = 0.dp) // Avoid double padding 
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    rideViewModel = rideViewModel,
                    profileViewModel = profileViewModel,
                    onNavigateToStart = { bottomNavController.navigate("main_start") },
                    onNavigateToJoin = { bottomNavController.navigate("main_join") },
                    onNavigateToMap = { bottomNavController.navigate("main_map") },
                    onSos = { rideViewModel.triggerSos() }
                )
            }
            composable(BottomNavItem.Start.route) {
                val currentLocation by rideViewModel.currentLocation.collectAsState()
                StartRideScreen(
                    rideActionState = rideActionState,
                    currentLocation = currentLocation,
                    onStart = { name, dest, lat, lng, invites, waypoints, isPublic ->
                        rideViewModel.startRide(name, dest, lat, lng, invites, waypoints, isPublic)
                    },
                    onNavigateToLocationSearch = { target ->
                        bottomNavController.currentBackStackEntry?.savedStateHandle?.set("location_target", target)
                        bottomNavController.navigate("main_location_search")
                    },
                    onNavigateToMap = { bottomNavController.navigate("main_map") }
                )
            }
            composable(BottomNavItem.Join.route) {
                JoinRideScreen(
                    rideActionState = rideActionState,
                    onJoin = { rideId ->
                        rideViewModel.joinRide(rideId)
                    },
                    onNavigateToMap = { bottomNavController.navigate("main_map") }
                )
            }
            composable(BottomNavItem.Map.route) {
                MapScreen(
                    viewModel = rideViewModel,
                    onNavigateToChat = { rideId -> bottomNavController.navigate("main_chat/$rideId") }
                )
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen(rideViewModel = rideViewModel)
            }
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    profileViewModel = profileViewModel,
                    onNavigateToSettings = { bottomNavController.navigate("main_settings") }
                )
            }
            composable("main_settings") { // Hidden from bottom bar
                SettingsScreen(
                    authViewModel = authViewModel,
                    profileViewModel = profileViewModel,
                    onLogout = {
                        rootNavController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { bottomNavController.popBackStack() },
                    onNavigateToProfileInfo = { bottomNavController.navigate("main_profile_info") }
                )
            }
            composable("main_profile_info") {
                ProfileInfoScreen(
                    profileViewModel = profileViewModel,
                    onBack = { bottomNavController.popBackStack() },
                    onNavigateToLocationSearch = {
                        bottomNavController.currentBackStackEntry?.savedStateHandle?.set("location_target", "profile")
                        bottomNavController.navigate("main_location_search")
                    }
                )
            }
            composable("main_chat/{rideId}") { backStackEntry ->
                val rideId = backStackEntry.arguments?.getString("rideId") ?: ""
                ChatScreen(
                    rideId = rideId,
                    chatViewModel = chatViewModel,
                    profileViewModel = profileViewModel,
                    onBack = { bottomNavController.popBackStack() }
                )
            }
            composable("main_location_search") { backStackEntry ->
                val target = bottomNavController.previousBackStackEntry?.savedStateHandle?.get<String>("location_target") ?: "origin"
                LocationSearchScreen(
                    onLocationSelected = { location, lat, lng ->
                        bottomNavController.previousBackStackEntry?.savedStateHandle?.set("selected_location", location)
                        bottomNavController.previousBackStackEntry?.savedStateHandle?.set("selected_lat", lat)
                        bottomNavController.previousBackStackEntry?.savedStateHandle?.set("selected_lng", lng)
                        bottomNavController.previousBackStackEntry?.savedStateHandle?.set("location_target", target)
                        bottomNavController.popBackStack()
                    },
                    onBack = { bottomNavController.popBackStack() }
                )
            }
        }
    }
}
