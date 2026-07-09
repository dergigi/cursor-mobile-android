package com.cursor.mobile.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.core.update.UpdateManager
import com.cursor.mobile.data.repository.LocalRemoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val updateManager: UpdateManager,
    private val localRemoteRepository: LocalRemoteRepository
) : ViewModel() {

    val themeModeFlow = apiKeyManager.themeModeFlow
    val biometricEnabledFlow = apiKeyManager.biometricEnabledFlow
    val connectionModeFlow = apiKeyManager.connectionModeFlow
    val relayBaseUrlFlow = apiKeyManager.relayBaseUrlFlow
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

    fun switchToCloudMode() {
        viewModelScope.launch {
            localRemoteRepository.logout()
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
