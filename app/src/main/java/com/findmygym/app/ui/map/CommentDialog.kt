package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.gyms.GymsRepository
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

@Composable
fun CommentDialog(
    gymId: String,
    onClose: () -> Unit
) {
    val repo = remember { GymsRepository() }
    val scope = rememberCoroutineScope()

    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }
    var ratingSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Gym actions") },
        text = {
            Column {
                // Rating section
                Text("Rate this gym", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..5).forEach { v ->
                        OutlinedButton(
                            enabled = !ratingSending,
                            onClick = {
                                error = null
                                ratingSending = true
                                scope.launch {
                                    try {
                                        repo.rateGym(gymId, v)
                                    } catch (e: Exception) {
                                        error = e.message ?: "Failed to rate"
                                    } finally {
                                        ratingSending = false
                                    }
                                }
                            },
                            modifier = Modifier.size(44.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("$v")
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Comment section
                Text("Add comment", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Comment") },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending && text.trim().isNotBlank(),
                onClick = {
                    sending = true
                    error = null
                    scope.launch {
                        try {
                            repo.addComment(gymId, text) // +2 points in repo transaction
                            text = ""
                            onClose()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to comment"
                        } finally {
                            sending = false
                        }
                    }
                }
            ) {
                Text(if (sending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !sending && !ratingSending,
                onClick = onClose
            ) { Text("Cancel") }
        }
    )
}