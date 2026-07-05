package com.cursor.mobile.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.security.ApiKeyManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) : ViewModel() {

    val themeModeFlow = apiKeyManager.themeModeFlow
    val biometricEnabledFlow = apiKeyManager.biometricEnabledFlow

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            apiKeyManager.saveThemeMode(mode)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            apiKeyManager.saveBiometricEnabled(enabled)
        }
    }
}
