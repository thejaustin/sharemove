package com.thejaustin.sharemove.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.thejaustin.sharemove.data.model.IntentCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("sharemove_prefs")

class PreferencesRepository(private val context: Context) {

    private fun hiddenKey(category: IntentCategory) =
        stringSetPreferencesKey("hidden_${category.name}")

    private fun disabledKey(category: IntentCategory) =
        stringSetPreferencesKey("disabled_${category.name}")

    fun hiddenPackages(category: IntentCategory): Flow<Set<String>> =
        context.dataStore.data.map { it[hiddenKey(category)] ?: emptySet() }

    fun disabledPackages(category: IntentCategory): Flow<Set<String>> =
        context.dataStore.data.map { it[disabledKey(category)] ?: emptySet() }

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
}
