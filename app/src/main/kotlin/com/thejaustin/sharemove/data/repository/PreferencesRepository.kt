package com.thejaustin.sharemove.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thejaustin.sharemove.data.model.HideMode
import com.thejaustin.sharemove.data.model.IntentCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("sharemove_prefs")

class PreferencesRepository(private val context: Context) {

    private fun hiddenKey(category: IntentCategory) =
        stringSetPreferencesKey("hidden_${category.name}")

    private fun disabledKey(category: IntentCategory) =
        stringSetPreferencesKey("disabled_${category.name}")

    private fun hiddenComponentsKey(category: IntentCategory) =
        stringSetPreferencesKey("hidden_components_${category.name}")

    private val hideModeKey = stringPreferencesKey("hide_mode")

    fun hiddenPackages(category: IntentCategory): Flow<Set<String>> =
        context.dataStore.data.map { it[hiddenKey(category)] ?: emptySet() }

    fun disabledPackages(category: IntentCategory): Flow<Set<String>> =
        context.dataStore.data.map { it[disabledKey(category)] ?: emptySet() }

    val hideMode: Flow<HideMode> = context.dataStore.data.map { prefs ->
        prefs[hideModeKey]
            ?.let { stored -> HideMode.entries.firstOrNull { it.name == stored } }
            ?: HideMode.SUSPEND
    }

    suspend fun setHideMode(mode: HideMode) {
        context.dataStore.edit { it[hideModeKey] = mode.name }
    }

    suspend fun setHidden(category: IntentCategory, packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val key = hiddenKey(category)
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (hidden) current + packageName else current - packageName
        }
    }

    suspend fun setDisabled(category: IntentCategory, packageName: String, disabled: Boolean) {
        context.dataStore.edit { prefs ->
            val key = disabledKey(category)
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (disabled) current + packageName else current - packageName
        }
    }

    fun hiddenComponents(category: IntentCategory): Flow<Set<String>> =
        context.dataStore.data.map { it[hiddenComponentsKey(category)] ?: emptySet() }

    /** Component recorded when [packageName] was hidden in COMPONENT mode, if any. */
    suspend fun hiddenComponentFor(category: IntentCategory, packageName: String): String? =
        context.dataStore.data.first()[hiddenComponentsKey(category)]
            ?.firstOrNull { it.substringBefore('/') == packageName }

    suspend fun setHiddenComponent(category: IntentCategory, componentName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val key = hiddenComponentsKey(category)
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (hidden) current + componentName else current - componentName
        }
    }
}
