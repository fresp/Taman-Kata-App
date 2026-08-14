package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.consentDataStore: DataStore<Preferences> by preferencesDataStore(name = "taman_kata_consent_prefs")

class ConsentPreferences(private val context: Context) {
    companion object {
        val KEY_HAS_CONSENTED = booleanPreferencesKey("has_consented")
        val KEY_CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
    }

    val hasConsented: Flow<Boolean> = context.consentDataStore.data.map { preferences ->
        preferences[KEY_HAS_CONSENTED] ?: false
    }

    val consentTimestamp: Flow<Long> = context.consentDataStore.data.map { preferences ->
        preferences[KEY_CONSENT_TIMESTAMP] ?: 0L
    }

    suspend fun saveConsent(consented: Boolean, timestamp: Long = System.currentTimeMillis()) {
        context.consentDataStore.edit { preferences ->
            preferences[KEY_HAS_CONSENTED] = consented
            preferences[KEY_CONSENT_TIMESTAMP] = if (consented) timestamp else 0L
        }
    }

    suspend fun revokeConsent() {
        context.consentDataStore.edit { preferences ->
            preferences[KEY_HAS_CONSENTED] = false
            preferences[KEY_CONSENT_TIMESTAMP] = 0L
        }
    }
}
