package com.findmygym.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findmygym.app.data.auth.AuthRepository
import com.findmygym.app.location.LocationTracker
import com.findmygym.app.nav.Routes
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MapScreen(onGoLeaderboard: () -> Unit) {
    val context = LocalContext.current
    val tracker = remember { LocationTracker(context) }
    val authRepo = remember { AuthRepository() }
    val vm: MapViewModel = viewModel()

    var hasPermission by remember { mutableStateOf(false) }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Add gym dialog state
    var showAdd by remember { mutableStateOf(false) }
    var gymName by remember { mutableStateOf("") }
    var gymType by remember { mutableStateOf("Gym") }
    var gymDesc by remember { mutableStateOf("") }

    var selectedGymId by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        hasPermission = ok
        if (!ok) error = "Location permission denied"
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    val fallback = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(fallback, 14f)
    }

    // Live updates + periodic Firestore update
    LaunchedEffect(hasPermission) {
        if (!hasPermission) return@LaunchedEffect
        error = null

        var lastSentMs = 0L

        try {
            tracker.locationUpdates().collectLatest { loc ->
                val ll = LatLng(loc.latitude, loc.longitude)
                myLatLng = ll
                cameraPositionState.position =
                    com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(ll, 16f)

                val now = System.currentTimeMillis()
                if (now - lastSentMs >= 10_000) { // every 10s
                    lastSentMs = now
                    try {
                        authRepo.updateMyLocation(loc.latitude, loc.longitude)
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            (error ?: vm.error)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasPermission)
            ) {
                myLatLng?.let {
                    Marker(state = MarkerState(position = it), title = "You")
                }

                // Gyms markers
                vm.gyms.forEach { g ->
                    Marker(
                        state = MarkerState(position = LatLng(g.lat, g.lng)),
                        title = g.name,
                        snippet = "${g.type} • by ${g.authorUsername}",
                        onClick = {
                            selectedGymId = g.id
                            true
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (myLatLng == null) {
                    error = "No location yet."
                } else {
                    showAdd = true
                }
            },
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("+")
        }

        FloatingActionButton(
            onClick = onGoLeaderboard,
            modifier = Modifier
                .align(androidx.compose.ui.Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text("🏆")
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val ll = myLatLng ?: return@TextButton
                        vm.addGym(
                            name = gymName,
                            type = gymType,
                            desc = gymDesc,
                            lat = ll.latitude,
                            lng = ll.longitude
                        ) {
                            gymName = ""
                            gymType = "Gym"
                            gymDesc = ""
                            showAdd = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("Cancel") }
            },
            title = { Text("Add gym here") },
            text = {
                Column {
                    OutlinedTextField(
                        value = gymName,
                        onValueChange = { gymName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gymType,
                        onValueChange = { gymType = it },
                        label = { Text("Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gymDesc,
                        onValueChange = { gymDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    selectedGymId?.let { gymId ->
        CommentDialog(
            gymId = gymId,
            onClose = { selectedGymId = null }
        )
    }
}