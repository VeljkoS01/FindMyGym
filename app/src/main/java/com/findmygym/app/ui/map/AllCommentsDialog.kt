package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.findmygym.app.data.gyms.GymsRepository
import com.findmygym.app.ui.components.fmgTextFieldTextStyle
import kotlinx.coroutines.launch

@Composable
fun AllCommentsDialog(
    gymId: String,
    gymName: String,
    onClose: () -> Unit
) {
    val repo = remember { GymsRepository() }
    val scope = rememberCoroutineScope()

    val comments by repo.streamComments(gymId).collectAsState(initial = emptyList())

    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(gymName.ifBlank { "Comments" }) },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(comments.reversed()) { c ->
                        Text(c.authorUsername.ifBlank { "User" }, style = MaterialTheme.typography.bodySmall)
                        Text(c.text)
                        Spacer(Modifier.height(10.dp))
                        Divider()
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Add comment", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Comment") },
                    textStyle = fmgTextFieldTextStyle(),
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
                            repo.addComment(gymId, text)
                            text = ""
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
            TextButton(enabled = !sending, onClick = onClose) { Text("Close") }
        }
    )
}
