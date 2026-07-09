package com.cursor.mobile.presentation.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.presentation.components.OpenTailscaleButton
import com.cursor.mobile.presentation.components.TailscaleButtonStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthenticated: (ConnectionMode) -> Unit,
    initialMode: ConnectionMode? = null,
    onBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPassword by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val fixedMode = initialMode

    LaunchedEffect(fixedMode) {
        fixedMode?.let { viewModel.setConnectionMode(it) }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onAuthenticated(uiState.connectionMode)
    }

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
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⌘",
                fontSize = 64.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Cursor Mobile",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Connect to your Cursor agents",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (fixedMode == null) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = uiState.connectionMode == ConnectionMode.CLOUD,
                        onClick = { viewModel.setConnectionMode(ConnectionMode.CLOUD) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Cloud Agents")
                    }
                    SegmentedButton(
                        selected = uiState.connectionMode == ConnectionMode.LOCAL_REMOTE,
                        onClick = { viewModel.setConnectionMode(ConnectionMode.LOCAL_REMOTE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Local Remote")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Text(
                    text = when (fixedMode) {
                        ConnectionMode.CLOUD -> "Sign in to Cloud Agents"
                        ConnectionMode.LOCAL_REMOTE -> "Connect to Local Remote"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            AnimatedContent(
                targetState = uiState.connectionMode,
                label = "auth_form"
            ) { mode ->
                when (mode) {
                    ConnectionMode.CLOUD -> CloudAuthForm(
                        apiKey = uiState.apiKey,
                        onApiKeyChange = viewModel::onApiKeyChange,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword }
                    )
                    ConnectionMode.LOCAL_REMOTE -> LocalAuthForm(
                        relayUrl = uiState.relayUrl,
                        relayPassword = uiState.relayPassword,
                        onRelayUrlChange = viewModel::onRelayUrlChange,
                        onRelayPasswordChange = viewModel::onRelayPasswordChange,
                        showPassword = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        onTestConnection = viewModel::testRelayConnection,
                        health = uiState.relayHealth
                    )
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.authenticate() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (uiState.connectionMode == ConnectionMode.CLOUD) "Connect with API Key" else "Connect to Relay",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (uiState.connectionMode == ConnectionMode.LOCAL_REMOTE) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Requires CursorRemote on your Mac + Tailscale. " +
                        "Use the Tailscale IP (e.g. http://100.64.x.x:3000).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CloudAuthForm(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = apiKey,
        onValueChange = onApiKeyChange,
        label = { Text("Cursor API Key") },
        placeholder = { Text("Paste your API key from Cursor settings") },
        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onTogglePassword) {
                Icon(
                    if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (showPassword) "Hide" else "Show"
                )
            }
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun LocalAuthForm(
    relayUrl: String,
    relayPassword: String,
    onRelayUrlChange: (String) -> Unit,
    onRelayPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePassword: () -> Unit,
    onTestConnection: () -> Unit,
    health: com.cursor.mobile.data.model.HealthResponse?
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        OpenTailscaleButton(style = TailscaleButtonStyle.TONAL)

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = relayUrl,
            onValueChange = onRelayUrlChange,
            label = { Text("Relay URL") },
            placeholder = { Text("http://100.64.x.x:3000") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = relayPassword,
            onValueChange = onRelayPasswordChange,
            label = { Text("Web Client Password") },
            placeholder = { Text("From CursorRemote setup panel") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "Hide" else "Show"
                    )
                }
            },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onTestConnection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Wifi, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Test Connection")
        }

        health?.let { h ->
            Spacer(modifier = Modifier.height(8.dp))
            val statusColor = when {
                h.connected -> MaterialTheme.colorScheme.primary
                h.sessionValid -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Text(
                text = buildString {
                    append("Health: OK")
                    if (h.connected) append(" | CDP connected")
                    else append(" | CDP disconnected")
                    append(" | ${h.messageCount} messages")
                },
                color = statusColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
