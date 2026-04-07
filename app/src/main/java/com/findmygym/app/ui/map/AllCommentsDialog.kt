package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import com.findmygym.app.ui.components.fmgTextFieldTextStyle

@Composable
fun AllCommentsDialog(
    gymId: String,
    gymName: String,
    onClose: () -> Unit
) {
    val viewModel: MapViewModel = viewModel()

    val comments = viewModel.comments
    val sending = viewModel.commentSending
    val error = viewModel.commentError

    var text by remember { mutableStateOf("") }

    LaunchedEffect(gymId) {
        viewModel.loadGymDetails(gymId)
    }

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
                    items(comments.reversed()) { comment ->
                        Text(
                            comment.authorUsername.ifBlank { "User" },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(comment.text)
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
                    val commentText = text.trim()
                    viewModel.addComment(gymId, commentText) {
                        text = ""
                    }
                }
            ) {
                Text(if (sending) "Sending..." else "Send")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    viewModel.clearCommentError()
                    onClose()
                }
            ) {
                Text("Close")
            }
        }
    )
}