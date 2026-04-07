package com.findmygym.app.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.findmygym.app.data.model.Gym
import com.findmygym.app.data.model.GymComment
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GymDetailsDialog(
    gym: Gym,
    onClose: () -> Unit
) {
    val viewModel: MapViewModel = viewModel()

    val comments = viewModel.comments
    val hasRated = viewModel.hasRated
    val ratingSending = viewModel.ratingSending
    val commentSending = viewModel.commentSending
    val error = viewModel.commentError

    var showAllComments by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(gym.id) {
        viewModel.loadGymDetails(gym.id)
    }

    val preview: List<GymComment> = remember(comments) {
        comments.takeLast(3).reversed()
    }

    val allSorted: List<GymComment> = remember(comments) {
        comments.reversed()
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(gym.name.ifBlank { "Gym" }) },
        text = {
            Column {

                Text(
                    gym.type.ifBlank { "Gym" },
                    style = MaterialTheme.typography.bodySmall
                )

                if (gym.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(gym.description)
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "⭐ ${"%.1f".format(gym.avgRating)} (${gym.ratingCount})",
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(Modifier.height(14.dp))

                when (hasRated) {
                    null -> Text("Checking rating...", style = MaterialTheme.typography.bodySmall)

                    true -> Text(
                        "You already rated this gym.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    false -> {
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
                                        viewModel.rateGym(gym.id, v)
                                    },
                                    modifier = Modifier.size(44.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("$v")
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text("Comments", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))

                if (comments.isEmpty()) {
                    Text("No comments yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (!showAllComments) {
                        preview.forEach { c ->
                            CommentRow(c)
                            Spacer(Modifier.height(6.dp))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                        ) {
                            items(allSorted) { c ->
                                CommentRow(c)
                                Spacer(Modifier.height(10.dp))
                                Divider()
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }

                TextButton(
                    onClick = { showAllComments = !showAllComments },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(if (showAllComments) "Hide" else "Show all")
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    label = { Text("Add a comment") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        enabled = !commentSending && commentText.trim().isNotBlank(),
                        onClick = {
                            val text = commentText.trim()

                            viewModel.addComment(gym.id, text) {
                                commentText = ""
                            }
                        }
                    ) {
                        Text(if (commentSending) "Posting..." else "Post")
                    }
                }

                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun CommentRow(c: GymComment) {
    val sdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val date = remember(c.createdAt) {
        sdf.format(Date(c.createdAt))
    }

    Column {
        Text(
            c.authorUsername.ifBlank { "User" },
            style = MaterialTheme.typography.bodySmall
        )
        Text(c.text)
        Text(date, style = MaterialTheme.typography.bodySmall)
    }
}