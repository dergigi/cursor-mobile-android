package com.cursor.mobile.data.repository

import com.cursor.mobile.core.network.RelayAuthService
import com.cursor.mobile.core.network.RelaySocketClient
import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.core.security.ConnectionMode
import com.cursor.mobile.data.model.CommandResult
import com.cursor.mobile.data.model.ConnectionStatus
import com.cursor.mobile.data.model.CursorState
import com.cursor.mobile.data.model.HealthResponse
import com.cursor.mobile.data.model.LoginResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalRemoteRepository @Inject constructor(
    private val apiKeyManager: ApiKeyManager,
    private val relayAuthService: RelayAuthService,
    private val socketClient: RelaySocketClient
) {
    val state: StateFlow<CursorState> = socketClient.state
    val connectionStatus: StateFlow<ConnectionStatus> = socketClient.connectionStatus
    val commandResults: StateFlow<Map<String, CommandResult>> = socketClient.commandResults

    suspend fun login(baseUrl: String, password: String): LoginResponse {
        val trimmedUrl = baseUrl.trim().trimEnd('/')
        val response = relayAuthService.loginWithUrl(trimmedUrl, password)
        if (response.token != null) {
            apiKeyManager.saveRelayCredentials(trimmedUrl, response.token)
            apiKeyManager.saveConnectionMode(ConnectionMode.LOCAL_REMOTE)
            socketClient.connect(response.token, trimmedUrl)
        }
        return response
    }

    suspend fun loginWithStoredCredentials() {
        val baseUrl = apiKeyManager.getRelayBaseUrl() ?: return
        val token = apiKeyManager.getRelaySessionToken() ?: return
        if (!socketClient.isConnected()) {
            socketClient.connect(token, baseUrl)
        }
    }

    suspend fun reconnect() {
        val baseUrl = apiKeyManager.getRelayBaseUrl() ?: return
        val token = apiKeyManager.getRelaySessionToken()
        if (token != null) {
            socketClient.disconnect()
            socketClient.connect(token, baseUrl)
        }
    }

    suspend fun testConnection(baseUrl: String, password: String): HealthResponse {
        val trimmedUrl = baseUrl.trim().trimEnd('/')
        val login = relayAuthService.loginWithUrl(trimmedUrl, password)
        if (login.token == null) {
            throw IllegalStateException(login.error ?: "Invalid password")
        }
        return relayAuthService.healthWithUrl(trimmedUrl)
    }

    suspend fun disconnect() {
        socketClient.disconnect()
    }

    suspend fun logout() {
        socketClient.disconnect()
        apiKeyManager.clearRelayCredentials()
        apiKeyManager.saveConnectionMode(ConnectionMode.CLOUD)
    }

    fun sendMessage(text: String) {
        socketClient.sendMessage(newCommandId(), text)
    }

    fun approve(selectorPath: String) {
        socketClient.approve(newCommandId(), selectorPath)
    }

    fun reject(selectorPath: String) {
        socketClient.reject(newCommandId(), selectorPath)
    }

    fun approveAll() {
        socketClient.approveAll(newCommandId())
    }

    fun runCommand(selectorPath: String) {
        socketClient.clickAction(newCommandId(), selectorPath, "run")
    }

    fun skipCommand(selectorPath: String) {
        socketClient.clickAction(newCommandId(), selectorPath, "skip")
    }

    fun allowCommand(selectorPath: String) {
        socketClient.clickAction(newCommandId(), selectorPath, "allow")
    }

    fun switchWindow(windowId: String) {
        socketClient.switchWindow(newCommandId(), windowId)
    }

    fun switchTab(tabSelectorPath: String) {
        socketClient.switchTab(newCommandId(), tabSelectorPath)
    }

    fun setMode(modeId: String) {
        socketClient.setMode(newCommandId(), modeId)
    }

    fun setModel(modelId: String) {
        socketClient.setModel(newCommandId(), modelId)
    }

    fun getModelOptions() {
        socketClient.getModelOptions(newCommandId())
    }

    fun viewPlan(selectorPath: String) {
        socketClient.getPlanFull(newCommandId(), selectorPath)
    }

    fun buildPlan(selectorPath: String) {
        socketClient.clickAction(newCommandId(), selectorPath, "build")
    }

    fun newChat() {
        socketClient.newChat(newCommandId())
    }

    fun consumeCommandResult(commandId: String): CommandResult? {
        return socketClient.commandResults.value[commandId]
    }

    private fun newCommandId(): String {
        return UUID.randomUUID().toString()
    }
}
