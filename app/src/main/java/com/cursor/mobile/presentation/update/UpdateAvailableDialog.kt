package com.cursor.mobile.presentation.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cursor.mobile.core.update.UpdateState

@Composable
fun UpdateAvailableDialog(
    state: UpdateState,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.ReadyToInstall -> state.info
        is UpdateState.Failed -> state.info
        else -> null
    }

    val title = when (state) {
        is UpdateState.Checking -> "Checking for updates"
        is UpdateState.UpToDate -> "Up to date"
        is UpdateState.Available -> "Update available v${info?.versionName ?: ""}"
        is UpdateState.Downloading -> "Downloading update"
        is UpdateState.ReadyToInstall -> "Ready to install v${info?.versionName ?: ""}"
        is UpdateState.Failed -> "Update failed"
        else -> "Update"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(title, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                when (state) {
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { state.progress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${(state.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    is UpdateState.Failed -> {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    is UpdateState.UpToDate -> {
                        Text("You're on the latest version.")
                    }

                    else -> {
                        info?.releaseNotes?.let { notes ->
                            if (notes.isNotBlank()) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 180.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    Text(notes)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateState.Available -> {
                    Button(onClick = onDownload) {
                        Text("Download")
                    }
                }

                is UpdateState.ReadyToInstall -> {
                    Button(onClick = onInstall) {
                        Text("Install")
                    }
                }

                is UpdateState.Failed -> {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }

                else -> {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}
