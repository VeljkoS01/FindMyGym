package com.findmygym.app.ui.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.findmygym.app.location.LocationTracker
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val tracker = remember { LocationTracker(context) }

    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Permission request
    val permissionLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
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

    LaunchedEffect(Unit) {
        try {
            val loc = tracker.getLastLocation()
            if (loc != null) {
                myLatLng = LatLng(loc.latitude, loc.longitude)
            } else {
                error = "No location yet. Try moving / enabling GPS."
            }
        } catch (e: Exception) {
            error = e.message
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
        }

        val start = myLatLng ?: LatLng(43.3209, 21.8958) // Nis fallback
        val cameraPositionState = rememberCameraPositionState {
            position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(start, 14f)
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = myLatLng != null)
        ) {
            myLatLng?.let {
                Marker(state = MarkerState(position = it), title = "You")
            }
        }
    }
}