package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.findmygym.app.ui.components.textFieldTextStyle

@Composable
fun MapFiltersDialog(
    initialQuery: String,
    initialRadiusKm: Int,
    onApply: (String, Int) -> Unit,
    onClear: () -> Unit,
    onCancel: () -> Unit
) {
    var q by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }
    var r by rememberSaveable(initialRadiusKm) { mutableIntStateOf(initialRadiusKm) }

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Search filters") },
        text = {
            Column {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    label = { Text("Name / Type / Description") },
                    textStyle = textFieldTextStyle(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))

                RadiusDropdown(
                    radiusKm = r,
                    onChange = { r = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    focusManager.clearFocus()
                    onApply(q, r)
                }
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        q = ""
                        r = 0
                        onClear()
                    }
                ) {
                    Text("Clear")
                }

                Spacer(Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        focusManager.clearFocus()
                        onCancel()
                    }
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}