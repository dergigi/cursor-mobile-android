package com.cursor.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.BiometricHelper
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.core.update.UpdateManager
import com.cursor.mobile.core.update.UpdateState
import com.cursor.mobile.presentation.auth.AuthViewModel
import com.cursor.mobile.presentation.theme.CursorMobileTheme
import com.cursor.mobile.presentation.update.UpdateAvailableDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var apiKeyManager: ApiKeyManager

    @Inject
    lateinit var biometricHelper: BiometricHelper

    @Inject
    lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val deepLinkAgentId = intent?.getStringExtra("agentId")

        setContent {
            val themeMode by apiKeyManager.themeModeFlow.collectAsState(initial = "system")
            val biometricEnabled by apiKeyManager.biometricEnabledFlow.collectAsState(initial = false)
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            var isAuthenticated by remember { mutableStateOf(false) }
            var showBiometricPrompt by remember { mutableStateOf(false) }
            var sessionUnlocked by remember { mutableStateOf(false) }

            CursorMobileTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.uiState.collectAsState()
                    val connectionMode by apiKeyManager.connectionModeFlow.collectAsState(initial = ConnectionMode.CLOUD)
                    val updateState by updateManager.state.collectAsState()
                    val updateDialogVisible by updateManager.dialogVisible.collectAsState()
                    val coroutineScope = rememberCoroutineScope()

                    LaunchedEffect(currentRoute) {
                        if (currentRoute == Routes.HOME) {
                            sessionUnlocked = false
                        }
                    }

                    LaunchedEffect(authState.isAuthenticated) {
                        isAuthenticated = authState.isAuthenticated
                        if (isAuthenticated) {
                            updateManager.autoCheck()
                        }
                    }

                    LaunchedEffect(
                        authState.isAuthenticated,
                        currentRoute,
                        biometricEnabled,
                        sessionUnlocked
                    ) {
                        val onProtectedScreen = currentRoute != null &&
                            currentRoute != Routes.HOME &&
                            !currentRoute.startsWith("auth")
                        if (
                            isAuthenticated &&
                            biometricEnabled &&
                            biometricHelper.canAuthenticate() &&
                            onProtectedScreen &&
                            !sessionUnlocked
                        ) {
                            showBiometricPrompt = true
                        }
                    }

                    LaunchedEffect(showBiometricPrompt) {
                        if (showBiometricPrompt) {
                            biometricHelper.showPrompt(
                                activity = this@MainActivity,
                                onSuccess = {
                                    sessionUnlocked = true
                                    showBiometricPrompt = false
                                },
                                onCancel = { finish() },
                                onError = { finish() }
                            )
                        }
                    }

                    // Handle deep link from notification
                    LaunchedEffect(authState.isAuthenticated, connectionMode, deepLinkAgentId) {
                        if (
                            authState.isAuthenticated &&
                            connectionMode == ConnectionMode.CLOUD &&
                            deepLinkAgentId != null
                        ) {
                            navController.navigate(Routes.detail(deepLinkAgentId))
                        }
                    }

                    if (updateDialogVisible) {
                        UpdateAvailableDialog(
                            state = updateState,
                            onDownload = {
                                val info = (updateState as? UpdateState.Available)?.info
                                    ?: (updateState as? UpdateState.Failed)?.info
                                info?.let {
                                    coroutineScope.launch { updateManager.download(it) }
                                }
                            },
                            onInstall = {
                                val ready = updateState as? UpdateState.ReadyToInstall
                                ready?.let { updateManager.install(this@MainActivity, it.apkFile) }
                            },
                            onRetry = {
                                coroutineScope.launch { updateManager.manualCheck() }
                            },
                            onDismiss = { updateManager.dismissUpdateDialog() }
                        )
                    }

                    AppNavHost(
                        navController = navController
                    )
                }
            }
        }
    }
}
