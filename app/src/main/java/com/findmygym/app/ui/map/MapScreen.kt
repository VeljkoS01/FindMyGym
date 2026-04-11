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
import com.findmygym.app.location.LocationTracker
import com.findmygym.app.notifications.NotificationHelper
import com.findmygym.app.ui.components.textFieldTextStyle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    requestGymList: Boolean,
    onRequestGymListConsumed: () -> Unit,
    requestFilters: Boolean,
    onRequestFiltersConsumed: () -> Unit,
    requestAddGym: Boolean,
    onRequestAddGymConsumed: () -> Unit,
    focusLat: Double?,
    focusLng: Double?,
    onFocusConsumed: () -> Unit
) {
    val context = LocalContext.current
    val tracker = remember(context) { LocationTracker(context) }
    val viewModel: MapViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var hasLocationPermission by remember { mutableStateOf(false) }
    var myLatLng by remember { mutableStateOf<LatLng?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }

    var hasCenteredOnMe by remember { mutableStateOf(false) }
    var selectedGymId by remember { mutableStateOf<String?>(null) }

    var showAdd by remember { mutableStateOf(false) }
    var gymName by remember { mutableStateOf("") }
    var gymType by remember { mutableStateOf("Gym") }
    var gymDesc by remember { mutableStateOf("") }

    var pickingLocation by remember { mutableStateOf(false) }
    var pendingLatLng by remember { mutableStateOf<LatLng?>(null) }

    var showGymList by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

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

    LaunchedEffect(requestGymList) {
        if (requestGymList) {
            showGymList = true
            onRequestGymListConsumed()
        }
    }

    LaunchedEffect(requestFilters) {
        if (requestFilters) {
            showFilters = true
            onRequestFiltersConsumed()
        }
    }

    LaunchedEffect(requestAddGym) {
        if (requestAddGym) {
            localError = null
            pendingLatLng = null
            pickingLocation = true
            showAdd = false
            onRequestAddGymConsumed()
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
        var lastUiUpdateMs = 0L

        try {
            tracker.locationUpdates().collect { loc ->
                val ll = LatLng(loc.latitude, loc.longitude)
                val now = System.currentTimeMillis()

                if (now - lastUiUpdateMs >= 1000) {
                    myLatLng = ll
                    lastUiUpdateMs = now
                }

                if (!hasCenteredOnMe) {
                    cameraPositionState.position =
                        com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(ll, 16f)
                    hasCenteredOnMe = true
                }

                if (now - lastSentMs >= 10_000) {
                    lastSentMs = now
                    viewModel.updateMyLocation(loc.latitude, loc.longitude)
                }
            }
        } catch (e: Exception) {
            localError = e.message
        }
    }

    LaunchedEffect(notifPermissionOk, myLatLng, viewModel.gyms) {
        if (!notifPermissionOk) return@LaunchedEffect

        val ll = myLatLng ?: return@LaunchedEffect
        NotificationHelper.ensureChannel(context)

        viewModel.checkNearbyGymsAndNotify(
            myLat = ll.latitude,
            myLng = ll.longitude
        ) { title, message ->
            NotificationHelper.showNearbyGym(
                context = context,
                title = title,
                message = message
            )
        }
    }

    LaunchedEffect(focusLat, focusLng) {
        if (focusLat != null && focusLng != null) {
            val target = LatLng(focusLat, focusLng)
            val update = CameraUpdateFactory.newLatLngZoom(target, 16f)
            cameraPositionState.animate(update, 700)
            onFocusConsumed()
        }
    }

    val filtered by remember(myLatLng, viewModel.gyms, viewModel.query, viewModel.radiusKm) {
        derivedStateOf {
            val my = myLatLng
            viewModel.filteredGyms(my?.latitude, my?.longitude)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        (localError ?: viewModel.error)?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
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
                    )
                }

                filtered.forEach { gym ->
                    Marker(
                        state = MarkerState(position = LatLng(gym.lat, gym.lng)),
                        title = gym.name,
                        snippet = "${gym.type} • ⭐ ${"%.1f".format(gym.avgRating)} (${gym.ratingCount})",
                        onClick = {
                            selectedGymId = gym.id
                            true
                        }
                    )
                }
            }

            if (pickingLocation) {
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("Tap on map to place the gym pin")
                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                pickingLocation = false
                                pendingLatLng = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    if (showGymList) {
        GymListDialog(
            gyms = viewModel.gyms,
            myLatLng = myLatLng,
            distanceKm = { gym ->
                val ll = myLatLng
                if (ll == null) Double.MAX_VALUE
                else viewModel.distanceKm(ll.latitude, ll.longitude, gym.lat, gym.lng)
            },
            onDismiss = { showGymList = false },
            onSelect = { gym ->
                showGymList = false
                scope.launch {
                    val target = LatLng(gym.lat, gym.lng)
                    val update = CameraUpdateFactory.newLatLngZoom(target, 16f)
                    cameraPositionState.animate(update, 700)
                }
            }
        )
    }

    if (showFilters) {
        MapFiltersDialog(
            initialQuery = viewModel.query,
            initialRadiusKm = viewModel.radiusKm,
            onApply = { query, radius ->
                viewModel.query = query
                viewModel.radiusKm = radius
                showFilters = false
            },
            onClear = {
                viewModel.query = ""
                viewModel.radiusKm = 0
            },
            onCancel = { showFilters = false }
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = {
                showAdd = false
                pendingLatLng = null
                pickingLocation = false
            },
            title = { Text("Add gym") },
            text = {
                Column {
                    pendingLatLng?.let { ll ->
                        Text(
                            text = "Pinned location: ${"%.5f".format(ll.latitude)}, ${"%.5f".format(ll.longitude)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = gymName,
                        onValueChange = { gymName = it },
                        label = { Text("Name") },
                        textStyle = textFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = gymType,
                        onValueChange = { gymType = it },
                        label = { Text("Type") },
                        textStyle = textFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = gymDesc,
                        onValueChange = { gymDesc = it },
                        label = { Text("Description") },
                        textStyle = textFieldTextStyle(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAdd = false
                        pendingLatLng = null
                        pickingLocation = false
                    }
                ) {
                    Text("Cancel")
                }
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

                        viewModel.addGym(
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
                ) {
                    Text("Add")
                }
            }
        )
    }

    val selectedGym = viewModel.gyms.firstOrNull { it.id == selectedGymId }
    selectedGym?.let { gym ->
        GymDetailsDialog(
            gym = gym,
            onClose = { selectedGymId = null }
        )
    }
}