package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.gyms.GymsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope

@Composable
fun CommentDialog(
    gymId: String,
    onClose: () -> Unit
) {
    val repo = remember { GymsRepository() }

    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Add comment") },
        text = {
            Column {
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
                    // Compose: pokreni coroutine
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        try {
                            repo.addComment(gymId, text)
                            text = ""
                            onClose()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to comment"
                        } finally {
                            sending = false
                        }
                    }
                }
            ) { Text(if (sending) "Sending..." else "Send") }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}