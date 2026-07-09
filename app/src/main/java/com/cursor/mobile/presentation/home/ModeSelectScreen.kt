package com.cursor.mobile.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ModeSelectScreen(
    onCloudSelected: () -> Unit,
    onCloudNeedsAuth: () -> Unit,
    onLocalSelected: () -> Unit,
    onLocalNeedsAuth: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: ModeSelectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "⌘",
                fontSize = 72.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Cursor Mobile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Choose how you want to connect",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
            )

            ModeCard(
                title = "Cloud Agents",
                description = "Control Cursor cloud agents via the official API. Create agents, review PRs, and chat from anywhere.",
                icon = Icons.Default.Cloud,
                actionLabel = if (uiState.hasCloudCredentials) "Continue" else "Sign in",
                onClick = {
                    viewModel.prepareCloudMode(
                        onReady = onCloudSelected,
                        onNeedsAuth = onCloudNeedsAuth
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            ModeCard(
                title = "Local Remote",
                description = "Connect to CursorRemote on your Mac over Tailscale. Monitor and approve your local agent sessions.",
                icon = Icons.Default.Lan,
                actionLabel = if (uiState.hasLocalCredentials) "Continue" else "Set up",
                subtitle = uiState.relayUrl?.let { "Saved: $it" },
                onClick = {
                    viewModel.prepareLocalMode(
                        onReady = onLocalSelected,
                        onNeedsAuth = onLocalNeedsAuth
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You can switch modes anytime from this screen or Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = onClick) {
                    Text(actionLabel)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
