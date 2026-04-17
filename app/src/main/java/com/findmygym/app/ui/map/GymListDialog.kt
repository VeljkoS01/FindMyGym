package com.findmygym.app.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.model.Gym
import com.google.android.gms.maps.model.LatLng
import kotlin.math.roundToInt

private enum class GymSort { RATING, DISTANCE, NAME }

@Composable
fun GymListDialog(
    gyms: List<Gym>,
    myLatLng: LatLng?,
    distanceKm: (Gym) -> Double,
    onDismiss: () -> Unit,
    onSelect: (Gym) -> Unit
) {
    //Po defaultu sortiranje po udaljenosti
    var sort by remember { mutableStateOf(GymSort.DISTANCE) }

    //Sortirana lista u odnosu na izabrani kriterijum
    val sorted = remember(gyms, sort, myLatLng) {
        when (sort) {
            GymSort.RATING -> gyms.sortedWith(
                compareByDescending<Gym> { it.avgRating }
                    .thenByDescending { it.ratingCount }
                    .thenBy { it.name.lowercase() }
            )
            GymSort.NAME -> gyms.sortedBy { it.name.lowercase() }
            GymSort.DISTANCE -> {
                //Ako nemamo lokaciju korisnika, distance sort nema smisla pa prelazimo na ime
                if (myLatLng == null) gyms.sortedBy { it.name.lowercase() }
                else gyms.sortedBy { distanceKm(it) }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gym list") },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //Clickable za odabir sortiranja
                    FilterChip(
                        selected = sort == GymSort.DISTANCE,
                        onClick = { sort = GymSort.DISTANCE },
                        enabled = myLatLng != null,
                        label = { Text("Distance") }
                    )
                    FilterChip(
                        selected = sort == GymSort.RATING,
                        onClick = { sort = GymSort.RATING },
                        label = { Text("Rating") }
                    )
                    FilterChip(
                        selected = sort == GymSort.NAME,
                        onClick = { sort = GymSort.NAME },
                        label = { Text("A-Z") }
                    )
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(sorted) { g ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                //Klik na teratanu vraca istu parent-u
                                .clickable { onSelect(g) }
                                .padding(vertical = 10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(g.name, style = MaterialTheme.typography.titleMedium)
                                    Text(g.type, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    "⭐ ${"%.1f".format(g.avgRating)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            //Ako imamo lokaciju korisnika prikazujemo i udaljenost
                            if (myLatLng != null) {
                                val d = distanceKm(g)
                                val pretty =
                                    if (d < 10) "${"%.1f".format(d)} km" else "${d.roundToInt()} km"
                                Spacer(Modifier.height(2.dp))
                                Text(pretty, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
