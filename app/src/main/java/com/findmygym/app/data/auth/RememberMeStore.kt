package com.findmygym.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "findmygym_prefs")

class RememberMeStore(private val context: Context) {

    private val keyRemember: Preferences.Key<Boolean> = booleanPreferencesKey("remember_me")

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[keyRemember] ?: false
    }

    suspend fun setRememberMe(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[keyRemember] = value
        }
    }
}
