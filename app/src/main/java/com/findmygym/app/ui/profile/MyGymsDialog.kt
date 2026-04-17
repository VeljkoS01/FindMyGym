package com.findmygym.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.model.Gym

@Composable
fun MyGymsDialog(
    gyms: List<Gym>,
    onDismiss: () -> Unit,
    onSelect: (Gym) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("My gyms") },
        text = {
            if (gyms.isEmpty()) {
                //Ako korisnik nema dodatih teretana prikazujemo poruku
                Text("You haven't added any gyms yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    items(gyms) { g ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                //Klik na teretanu prosledjuje teretanu i fokusira se na Mapi
                                .clickable { onSelect(g) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(g.name.ifBlank { "Gym" }, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(g.type.ifBlank { "Gym" }, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            //Prikaz prosecne ocene i broja ocena
                            Text(
                                "⭐ ${"%.1f".format(g.avgRating)} (${g.ratingCount})",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        //Dugme za zatvaranje dijaloga
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
