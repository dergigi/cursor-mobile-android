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
import androidx.navigation.compose.rememberNavController
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.BiometricHelper
import com.cursor.mobile.presentation.auth.AuthViewModel
import com.cursor.mobile.presentation.theme.CursorMobileTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var apiKeyManager: ApiKeyManager

    @Inject
    lateinit var biometricHelper: BiometricHelper

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

            CursorMobileTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val authState by authViewModel.uiState.collectAsState()

                    LaunchedEffect(authState.isAuthenticated) {
                        isAuthenticated = authState.isAuthenticated
                        if (isAuthenticated && biometricEnabled && biometricHelper.canAuthenticate()) {
                            showBiometricPrompt = true
                        }
                    }

                    LaunchedEffect(showBiometricPrompt) {
                        if (showBiometricPrompt) {
                            biometricHelper.showPrompt(
                                activity = this@MainActivity,
                                onSuccess = { showBiometricPrompt = false },
                                onCancel = { finish() },
                                onError = { finish() }
                            )
                        }
                    }

                    // Handle deep link from notification
                    LaunchedEffect(authState.isAuthenticated, deepLinkAgentId) {
                        if (authState.isAuthenticated && deepLinkAgentId != null) {
                            navController.navigate(Routes.detail(deepLinkAgentId))
                        }
                    }

                    AppNavHost(
                        navController = navController,
                        isAuthenticated = authState.isAuthenticated
                    )
                }
            }
        }
    }
}
