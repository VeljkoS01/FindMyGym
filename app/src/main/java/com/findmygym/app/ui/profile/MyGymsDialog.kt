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
                                .clickable { onSelect(g) }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(g.name.ifBlank { "Gym" }, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(2.dp))
                            Text(g.type.ifBlank { "Gym" }, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
