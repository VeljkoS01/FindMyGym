package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RadiusDropdown(
    radiusKm: Int,
    onChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf(0, 1, 3, 5, 10)
    val label = if (radiusKm == 0) "Any distance" else "$radiusKm km"

    Column(Modifier.fillMaxWidth()) {

        Box(Modifier.fillMaxWidth()) {
            // Disabled field (doesn't consume clicks/focus)
            OutlinedTextField(
                value = label,
                onValueChange = {},
                label = { Text("Radius") },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = { Text("▼") }
            )

            // Transparent clickable overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = true }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { v ->
                DropdownMenuItem(
                    text = { Text(if (v == 0) "Any distance" else "$v km") },
                    onClick = {
                        onChange(v)
                        expanded = false
                    }
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Filter gyms by distance from your current location.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
