package com.cursor.mobile.core.network

import com.cursor.mobile.core.security.ApiKeyManager
import com.cursor.mobile.data.model.CommandPayload
import com.cursor.mobile.data.model.CommandResult
import com.cursor.mobile.data.model.ConnectionStatus
import com.cursor.mobile.data.model.CursorState
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import timber.log.Timber
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RelaySocketClient @Inject constructor(
    private val apiKeyManager: ApiKeyManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var socket: Socket? = null

    private val _state = MutableStateFlow(CursorState())
    val state: StateFlow<CursorState> = _state.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus(false))
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _commandResults = MutableStateFlow<Map<String, CommandResult>>(emptyMap())
    val commandResults: StateFlow<Map<String, CommandResult>> = _commandResults.asStateFlow()

    private val pendingCommandIds = mutableSetOf<String>()

    fun connect(token: String, baseUrl: String) {
        disconnect()

        val uri = URI.create(baseUrl)
        val options = IO.Options.builder()
            .setAuth(mapOf("token" to token))
            .setTransports(arrayOf("websocket", "polling"))
            .setTimeout(10_000)
            .setReconnectionAttempts(10)
            .setReconnectionDelay(1_000)
            .setReconnectionDelayMax(10_000)
            .build()

        val socket = IO.socket(uri, options)
        this.socket = socket

        socket.on(Socket.EVENT_CONNECT) {
            Timber.tag("RelaySocket").i("Connected to %s", baseUrl)
            _connectionStatus.value = ConnectionStatus(true)
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Timber.tag("RelaySocket").i("Disconnected")
            _connectionStatus.value = ConnectionStatus(false, "disconnected")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            val error = args.firstOrNull()?.toString() ?: "unknown"
            Timber.tag("RelaySocket").e("Connect error: %s", error)
            _connectionStatus.value = ConnectionStatus(false, error)
        }

        socket.on("state:full") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            try {
                val newState = json.decodeFromString(CursorState.serializer(), data.toString())
                _state.value = newState
            } catch (e: Exception) {
                Timber.tag("RelaySocket").e(e, "Failed to parse state:full")
            }
        }

        socket.on("state:patch") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            applyPatch(data.toString())
        }

        socket.on("connection:status") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            val connected = data.optBoolean("connected", false)
            val reason = data.optString("reason").takeIf { it.isNotBlank() }
            _connectionStatus.value = ConnectionStatus(connected, reason)
        }

        socket.on("command:result") { args ->
            val data = args.firstOrNull() as? JSONObject ?: return@on
            try {
                val result = json.decodeFromString(CommandResult.serializer(), data.toString())
                pendingCommandIds.remove(result.commandId)
                _commandResults.update { it + (result.commandId to result) }
            } catch (e: Exception) {
                Timber.tag("RelaySocket").e(e, "Failed to parse command:result")
            }
        }

        socket.connect()
    }

    fun disconnect() {
        socket?.let {
            it.off()
            it.disconnect()
        }
        socket = null
        _connectionStatus.value = ConnectionStatus(false)
    }

    fun isConnected(): Boolean {
        return socket?.connected() == true
    }

    fun sendMessage(commandId: String, text: String) {
        emit("command:send_message", CommandPayload(commandId = commandId, text = text))
    }

    fun approve(commandId: String, selectorPath: String) {
        emit("command:approve", CommandPayload(commandId = commandId, selectorPath = selectorPath))
    }

    fun reject(commandId: String, selectorPath: String) {
        emit("command:reject", CommandPayload(commandId = commandId, selectorPath = selectorPath))
    }

    fun approveAll(commandId: String) {
        emit("command:approve_all", CommandPayload(commandId = commandId))
    }

    fun clickAction(commandId: String, selectorPath: String, actionType: String? = null) {
        emit(
            "command:click_action",
            CommandPayload(
                commandId = commandId,
                selectorPath = selectorPath,
                actionType = actionType
            )
        )
    }

    fun switchWindow(commandId: String, windowId: String) {
        emit("command:switch_window", CommandPayload(commandId = commandId, windowId = windowId))
    }

    fun switchTab(commandId: String, tabSelectorPath: String) {
        emit("command:switch_tab", CommandPayload(commandId = commandId, tabSelectorPath = tabSelectorPath))
    }

    fun setMode(commandId: String, modeId: String) {
        emit("command:set_mode", CommandPayload(commandId = commandId, modeId = modeId))
    }

    fun setModel(commandId: String, modelId: String) {
        emit("command:set_model", CommandPayload(commandId = commandId, modelId = modelId))
    }

    fun getModelOptions(commandId: String) {
        emit("command:get_model_options", CommandPayload(commandId = commandId))
    }

    fun getPlanFull(commandId: String, selectorPath: String) {
        emit("command:get_plan_full", CommandPayload(commandId = commandId, selectorPath = selectorPath))
    }

    fun getPlanModelOptions(commandId: String, selectorPath: String) {
        emit(
            "command:get_plan_model_options",
            CommandPayload(commandId = commandId, selectorPath = selectorPath)
        )
    }

    fun setPlanModel(commandId: String, selectorPath: String, modelId: String) {
        emit(
            "command:set_plan_model",
            CommandPayload(commandId = commandId, selectorPath = selectorPath, modelId = modelId)
        )
    }

    fun newChat(commandId: String) {
        emit("command:new_chat", CommandPayload(commandId = commandId))
    }

    private fun emit(event: String, payload: CommandPayload) {
        pendingCommandIds.add(payload.commandId)
        val jsonString = json.encodeToString(CommandPayload.serializer(), payload)
        val jsonObject = org.json.JSONObject(jsonString)
        socket?.emit(event, jsonObject)
    }

    private fun applyPatch(jsonString: String) {
        try {
            val patch = json.parseToJsonElement(jsonString).jsonObject
            _state.update { current ->
                mergePatch(current, patch)
            }
        } catch (e: Exception) {
            Timber.tag("RelaySocket").e(e, "Failed to apply state:patch")
        }
    }

    private fun mergePatch(state: CursorState, patch: JsonObject): CursorState {
        val patchJson = json.encodeToString(JsonObject.serializer(), patch)
        val stateJson = json.encodeToString(CursorState.serializer(), state)
        val stateObj = org.json.JSONObject(stateJson)
        val patchObj = org.json.JSONObject(patchJson)

        val keys = patchObj.keys()
        while (keys.hasNext()) {
            val key = keys.next() as String
            val value = patchObj.get(key)
            stateObj.put(key, value)
        }

        return json.decodeFromString(CursorState.serializer(), stateObj.toString())
    }
}
