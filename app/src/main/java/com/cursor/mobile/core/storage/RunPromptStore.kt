package com.cursor.mobile.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.runPromptsDataStore: DataStore<Preferences> by preferencesDataStore(name = "run_prompts")

/**
 * The Cursor API does not return the prompt text a user typed for a run, so the
 * chat history can only show run IDs after reopening. This store keeps a local
 * copy of each prompt keyed by run ID so past messages remain readable.
 */
@Singleton
class RunPromptStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun savePrompt(runId: String, prompt: String) {
        if (runId.isBlank() || prompt.isBlank()) return
        context.runPromptsDataStore.edit { it[stringPreferencesKey(runId)] = prompt }
    }

    suspend fun getPrompts(runIds: List<String>): Map<String, String> {
        if (runIds.isEmpty()) return emptyMap()
        val prefs = context.runPromptsDataStore.data.first()
        return runIds.mapNotNull { id ->
            prefs[stringPreferencesKey(id)]?.let { id to it }
        }.toMap()
    }
}
