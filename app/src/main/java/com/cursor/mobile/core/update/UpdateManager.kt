package com.cursor.mobile.core.update

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.cursor.mobile.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String = ""
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateState
    data class Failed(val info: UpdateInfo?, val message: String) : UpdateState
}

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    @Volatile
    private var autoChecked = false

    companion object {
        private const val MANIFEST_URL =
            "https://github.com/chandradcp/cursor-mobile-android/releases/latest/download/update.json"
        private const val APK_NAME = "update.apk"
        private const val UPDATE_DIR = "updates"
    }

    fun showUpdateDialog() {
        _dialogVisible.value = true
    }

    fun dismissUpdateDialog() {
        _dialogVisible.value = false
    }

    suspend fun autoCheck() {
        if (autoChecked) return
        autoChecked = true
        checkInternal(silent = true)
    }

    suspend fun manualCheck() {
        checkInternal(silent = false)
    }

    private suspend fun checkInternal(silent: Boolean) {
        _state.update { UpdateState.Checking }
        if (!silent) _dialogVisible.value = true

        try {
            val info = fetchUpdateInfo()
            if (info.versionCode > BuildConfig.VERSION_CODE) {
                _state.update { UpdateState.Available(info) }
                _dialogVisible.value = true
            } else {
                _state.update { UpdateState.UpToDate }
                if (!silent) _dialogVisible.value = true
            }
        } catch (e: Exception) {
            if (silent) {
                _state.update { UpdateState.Idle }
            } else {
                _state.update { UpdateState.Failed(null, e.message ?: "Check failed") }
            }
        }
    }

    private suspend fun fetchUpdateInfo(): UpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(MANIFEST_URL)
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response")
            json.decodeFromString(UpdateInfo.serializer(), body)
        }
    }

    suspend fun download(info: UpdateInfo) {
        _state.update { UpdateState.Downloading(info, 0f) }

        try {
            val apkFile = File(context.cacheDir, "$UPDATE_DIR/$APK_NAME").also {
                it.parentFile?.mkdirs()
            }

            val request = Request.Builder()
                .url(info.apkUrl)
                .header("Accept", "application/vnd.android.package-archive")
                .build()

            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val totalBytes = body.contentLength().takeIf { it > 0 } ?: -1L

                    body.byteStream().use { input ->
                        apkFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var downloaded = 0L
                            var read: Int

                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (totalBytes > 0) {
                                    val progress = downloaded.toFloat() / totalBytes.toFloat()
                                    _state.update { UpdateState.Downloading(info, progress) }
                                }
                            }
                        }
                    }
                }
            }

            _state.update { UpdateState.ReadyToInstall(info, apkFile) }
        } catch (e: Exception) {
            _state.update { UpdateState.Failed(info, e.message ?: "Download failed") }
        }
    }

    fun install(activity: Activity, apkFile: File) {
        val currentInfo = when (val s = _state.value) {
            is UpdateState.ReadyToInstall -> s.info
            else -> null
        }

        if (!apkFile.exists()) {
            val info = currentInfo
            if (info != null) {
                _state.update { UpdateState.Available(info) }
            } else {
                _state.update { UpdateState.Failed(null, "Downloaded APK not found") }
            }
            return
        }

        val packageManager = activity.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${activity.packageName}")
                )
                try {
                    activity.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
                return
            }
        }

        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _state.update { UpdateState.Failed(currentInfo, "No installer found") }
        }
    }
}
