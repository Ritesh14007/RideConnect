package com.rideconnect.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
    }

    private lateinit var viewModel: com.rideconnect.app.viewmodel.RideViewModel
    private lateinit var authViewModel: com.rideconnect.app.viewmodel.AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = androidx.lifecycle.ViewModelProvider(this)[com.rideconnect.app.viewmodel.RideViewModel::class.java]
        authViewModel = androidx.lifecycle.ViewModelProvider(this)[com.rideconnect.app.viewmodel.AuthViewModel::class.java]
        
        checkPermissions()
        
        // Initialize Google Places
        if (!com.google.android.libraries.places.api.Places.isInitialized()) {
            val apiKey = getString(R.string.google_maps_key)
            com.google.android.libraries.places.api.Places.initialize(applicationContext, apiKey)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RideConnectApp(viewModel, authViewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (!permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            requestPermissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun RideConnectApp(
    viewModel: com.rideconnect.app.viewmodel.RideViewModel,
    authViewModel: com.rideconnect.app.viewmodel.AuthViewModel
) {
    val rootNavController = rememberNavController()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) "main" else "login"

    NavHost(navController = rootNavController, startDestination = startDestination) {
        composable("login") {
            com.rideconnect.app.ui.LoginScreen(
                authViewModel = authViewModel,
                onNavigateToHome = {
                    rootNavController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            com.rideconnect.app.ui.MainScreen(
                rideViewModel = viewModel,
                authViewModel = authViewModel,
                rootNavController = rootNavController
            )
        }
    }
}

