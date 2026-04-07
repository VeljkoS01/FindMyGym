package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun CommentDialog(
    gymId: String,
    onClose: () -> Unit
) {
    val viewModel: MapViewModel = viewModel()

    val hasRated = viewModel.hasRated
    val error = viewModel.commentError
    val sending = viewModel.commentSending
    val ratingSending = viewModel.ratingSending

    var text by remember { mutableStateOf("") }

    LaunchedEffect(gymId) {
        viewModel.loadGymDetails(gymId)
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Gym actions") },
        text = {
            Column {
                when (hasRated) {
                    null -> {
                        Text("Checking rating...", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                    }

                    true -> {
                        Text("You already rated this gym.", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                    }

                    false -> {
                        Text("Rate this gym", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..5).forEach { value ->
                                OutlinedButton(
                                    enabled = !ratingSending,
                                    onClick = {
                                        viewModel.rateGym(gymId, value)
                                    },
                                    modifier = Modifier.size(44.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("$value")
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                    }
                }

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
                    val commentText = text.trim()
                    viewModel.addComment(gymId, commentText) {
                        text = ""
                        onClose()
                    }
                }
            ) {
                Text(if (sending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !sending && !ratingSending,
                onClick = {
                    viewModel.clearCommentError()
                    onClose()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}