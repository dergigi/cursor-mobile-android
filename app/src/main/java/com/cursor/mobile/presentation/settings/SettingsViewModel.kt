package com.cursor.mobile.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.update.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val updateManager: UpdateManager
) : ViewModel() {

    val themeModeFlow = apiKeyManager.themeModeFlow
    val biometricEnabledFlow = apiKeyManager.biometricEnabledFlow
    val updateState = updateManager.state
    val updateDialogVisible = updateManager.dialogVisible

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

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.manualCheck()
        }
    }

    fun showUpdateDialog() {
        updateManager.showUpdateDialog()
    }

    fun dismissUpdateDialog() {
        updateManager.dismissUpdateDialog()
    }
}
