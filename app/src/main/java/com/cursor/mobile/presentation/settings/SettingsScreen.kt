package com.cursor.mobile.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cursor.mobile.BuildConfig
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.core.update.UpdateState
import com.cursor.mobile.presentation.auth.AuthViewModel
import com.cursor.mobile.presentation.components.OpenTailscaleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onChooseMode: () -> Unit = {},
    onMcpServersClick: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val themeMode by settingsViewModel.themeModeFlow.collectAsState(initial = "system")
    val biometricEnabled by settingsViewModel.biometricEnabledFlow.collectAsState(initial = false)
    val connectionMode by settingsViewModel.connectionModeFlow.collectAsState(initial = ConnectionMode.CLOUD)
    val relayBaseUrl by settingsViewModel.relayBaseUrlFlow.collectAsState(initial = null)
    val updateState by settingsViewModel.updateState.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSwitchModeDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Disconnect") },
            text = { Text("Remove saved credentials and return to the mode selection screen?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.logout()
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSwitchModeDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchModeDialog = false },
            title = { Text("Switch to Cloud Mode") },
            text = { Text("This will disconnect from the local relay and clear relay credentials. You can choose a mode again on the home screen.") },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.switchToCloudMode()
                    showSwitchModeDialog = false
                    onChooseMode()
                }) {
                    Text("Switch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchModeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentMode = themeMode,
            onModeSelected = { mode ->
                settingsViewModel.setThemeMode(mode)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Account card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            val modeLabel = when (connectionMode) {
                                ConnectionMode.CLOUD -> "Cloud Agents"
                                ConnectionMode.LOCAL_REMOTE -> "Local Remote"
                            }
                            Text(
                                authState.userName ?: modeLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (connectionMode == ConnectionMode.CLOUD) {
                                Text(
                                    "API Key: ****${authState.apiKey.takeLast(4).ifBlank { "..." }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    relayBaseUrl ?: "Local relay",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            authState.plan?.let { plan ->
                                val tierText = plan.tier?.let { " ($it)" } ?: ""
                                val planColor = if (plan.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                Text(
                                    "Plan: ${plan.name}$tierText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = planColor
                                )
                            }
                        }
                }
            }

            // Connection mode
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Connection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onChooseMode() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Choose mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Return to Cloud vs Local Remote selection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (connectionMode == ConnectionMode.LOCAL_REMOTE) {
                                    showSwitchModeDialog = true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lan,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mode", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                when (connectionMode) {
                                    ConnectionMode.CLOUD -> "Cloud Agents API"
                                    ConnectionMode.LOCAL_REMOTE -> "Local Remote via CursorRemote"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (connectionMode == ConnectionMode.LOCAL_REMOTE) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (connectionMode == ConnectionMode.LOCAL_REMOTE) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        OpenTailscaleButton()
                    }
                }
            }

            // Appearance & Security
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showThemeDialog = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Theme", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                themeMode.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Biometric lock", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { settingsViewModel.setBiometricEnabled(it) }
                        )
                    }
                }
            }

            // Integrations
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Integrations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMcpServersClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MCP Servers", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Query Datadog, Slack, and other tools",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Info
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val updateSubtitle = when (val s = updateState) {
                        is UpdateState.Checking -> "Checking…"
                        is UpdateState.UpToDate -> "You’re on the latest version"
                        is UpdateState.Available -> "Version ${s.info.versionName} available"
                        is UpdateState.Downloading -> "Downloading ${(s.progress * 100).toInt()}%…"
                        is UpdateState.ReadyToInstall -> "Version ${s.info.versionName} ready to install"
                        is UpdateState.Failed -> "Check failed — tap to retry"
                        else -> "Installed: v${BuildConfig.VERSION_NAME}"
                    }
                    val updateSubtitleColor = when (updateState) {
                        is UpdateState.Available,
                        is UpdateState.ReadyToInstall -> MaterialTheme.colorScheme.primary
                        is UpdateState.Failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (updateState) {
                                    is UpdateState.Available,
                                    is UpdateState.ReadyToInstall -> settingsViewModel.showUpdateDialog()
                                    else -> settingsViewModel.checkForUpdates()
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Check for updates", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                updateSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = updateSubtitleColor
                            )
                        }
                        if (updateState is UpdateState.Checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Cursor Mobile v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Control your Cursor AI coding agents from anywhere. Uses the Cloud Agents API v1.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Disconnect button
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnect", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
