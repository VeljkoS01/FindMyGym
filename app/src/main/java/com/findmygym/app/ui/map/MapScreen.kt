package com.findmygym.app.ui.map

import android.Manifest
import android.os.Build
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
import com.findmygym.app.notifications.NotificationHelper
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.findmygym.app.ui.components.fmgTextFieldTextStyle


@Composable
fun MapScreen(
    onGoLeaderboard: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember(context) { LocationTracker(context) }
    val authRepo = remember { AuthRepository() }
    val vm: MapViewModel = viewModel()

    var hasLocationPermission by remember { mutableStateOf(false) }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    var selectedGymId by remember { mutableStateOf<String?>(null) }

    var showAdd by remember { mutableStateOf(false) }
    var gymName by remember { mutableStateOf("") }
    var gymType by remember { mutableStateOf("Gym") }
    var gymDesc by remember { mutableStateOf("") }

    // picking mode + pending pin
    var pickingLocation by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }

    // notifications permission (Android 13+)
    var notifPermissionOk by remember { mutableStateOf(Build.VERSION.SDK_INT < 33) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionOk = granted
    }

    val locationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        hasLocationPermission = ok
        if (!ok) localError = "Location permission denied"
    }

    LaunchedEffect(Unit) {
        locationPermLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        if (Build.VERSION.SDK_INT >= 33) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val fallback = LatLng(43.3209, 21.8958)
    val cameraPositionState = rememberCameraPositionState {
        position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(fallback, 13f)
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@LaunchedEffect
        localError = null

        var lastSentMs = 0L
        try {
            tracker.locationUpdates().collectLatest { loc ->
                val ll = LatLng(loc.latitude, loc.longitude)
                myLatLng = ll

                cameraPositionState.position =
                    com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(ll, 16f)

                val now = System.currentTimeMillis()
                if (now - lastSentMs >= 10_000) {
                    lastSentMs = now
                    try {
                        authRepo.updateMyLocation(loc.latitude, loc.longitude)
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            localError = e.message
        }
    }

    var lastNotifiedGymId by remember { mutableStateOf<String?>(null) }
    var lastNotifiedAt by remember { mutableStateOf(0L) }

    LaunchedEffect(notifPermissionOk, vm.gyms) {
        if (!notifPermissionOk) return@LaunchedEffect
        NotificationHelper.ensureChannel(context)

        while (true) {
            val ll = myLatLng
            if (ll != null) {
                val near = vm.gyms
                    .map { g -> g to vm.distanceKm(ll.latitude, ll.longitude, g.lat, g.lng) }
                    .filter { it.second <= 0.2 }
                    .minByOrNull { it.second }
                    ?.first

                val now = System.currentTimeMillis()
                val cooldownOk = now - lastNotifiedAt > 3 * 60 * 1000

                if (near != null && (near.id != lastNotifiedGymId || cooldownOk)) {
                    lastNotifiedGymId = near.id
                    lastNotifiedAt = now

                    NotificationHelper.showNearbyGym(
                        context = context,
                        title = "Gym nearby",
                        message = "${near.name} is close to you"
                    )
                }
            }
            delay(15_000)
        }
    }

    val my = myLatLng
    val filtered = vm.filteredGyms(my?.latitude, my?.longitude)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        (localError ?: vm.error)?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = vm.query,
                onValueChange = { vm.query = it },
                label = { Text("Search gyms") },
                textStyle = fmgTextFieldTextStyle(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            RadiusDropdown(
                radiusKm = vm.radiusKm,
                onChange = { vm.radiusKm = it }
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),

                onMapClick = { ll ->
                    if (pickingLocation) {
                        pendingLatLng = ll
                        showAdd = true
                        pickingLocation = false
                    }
                },

                onMapLongClick = { ll ->
                    if (pickingLocation) {
                        pendingLatLng = ll
                        showAdd = true
                        pickingLocation = false
                    }
                }
            ) {

                pendingLatLng?.let { ll ->
                    Marker(
                        state = MarkerState(position = ll),
                        title = "New gym here"
                    )
                }

                filtered.forEach { g ->
                    Marker(
                        state = MarkerState(position = LatLng(g.lat, g.lng)),
                        title = g.name,
                        snippet = "${g.type} • ⭐ ${"%.1f".format(g.avgRating)} (${g.ratingCount})",
                        onClick = {
                            selectedGymId = g.id
                            true
                        }
                    )
                }
            }

            // small hint when choosing location
            if (pickingLocation) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    Text(
                        "Tap on map to place the gym pin",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            FloatingActionButton(
                onClick = onGoLeaderboard,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("🏆")
            }

            FloatingActionButton(
                onClick = {
                    // ✅ + starts "pick location" mode, NOT my location
                    localError = null
                    pendingLatLng = null
                    pickingLocation = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text("+")
            }
        }

        if (filtered.isNotEmpty()) {
            Divider()
            Text(
                "Results (${filtered.size})",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall
            )

            filtered.take(10).forEach { g ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(g.name)
                        Text(g.type, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("⭐ ${"%.1f".format(g.avgRating)}", style = MaterialTheme.typography.bodySmall)
                }
                Divider()
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = {
                showAdd = false
                pendingLatLng = null
                pickingLocation = false
            },
            confirmButton = {
                TextButton(
                    enabled = gymName.trim().isNotBlank(),
                    onClick = {
                        val target = pendingLatLng
                        if (target == null) {
                            localError = "Please pick a location on the map first."
                            showAdd = false
                            return@TextButton
                        }

                        vm.addGym(
                            name = gymName,
                            type = gymType,
                            desc = gymDesc,
                            lat = target.latitude,
                            lng = target.longitude
                        ) {
                            gymName = ""
                            gymType = "Gym"
                            gymDesc = ""
                            showAdd = false
                            pendingLatLng = null
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAdd = false
                    pendingLatLng = null
                    pickingLocation = false
                }) { Text("Cancel") }
            },
            title = { Text("Add gym") },
            text = {
                Column {
                    pendingLatLng?.let { ll ->
                        Text(
                            "Pinned location: ${"%.5f".format(ll.latitude)}, ${"%.5f".format(ll.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = gymName,
                        onValueChange = { gymName = it },
                        label = { Text("Name") },
                        textStyle = fmgTextFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gymType,
                        onValueChange = { gymType = it },
                        label = { Text("Type") },
                        textStyle = fmgTextFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gymDesc,
                        onValueChange = { gymDesc = it },
                        label = { Text("Description") },
                        textStyle = fmgTextFieldTextStyle(),
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
