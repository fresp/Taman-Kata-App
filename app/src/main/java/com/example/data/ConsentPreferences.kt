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

open class ConsentPreferences(private val context: Context? = null) {
    companion object {
        val KEY_HAS_CONSENTED = booleanPreferencesKey("has_consented")
        val KEY_CONSENT_TIMESTAMP = longPreferencesKey("consent_timestamp")
    }

    open val hasConsented: Flow<Boolean> = context?.let { ctx ->
        ctx.consentDataStore.data.map { preferences ->
            preferences[KEY_HAS_CONSENTED] ?: false
        }
    } ?: kotlinx.coroutines.flow.flowOf(false)

    open val consentTimestamp: Flow<Long> = context?.let { ctx ->
        ctx.consentDataStore.data.map { preferences ->
            preferences[KEY_CONSENT_TIMESTAMP] ?: 0L
        }
    } ?: kotlinx.coroutines.flow.flowOf(0L)

    open suspend fun saveConsent(consented: Boolean, timestamp: Long = System.currentTimeMillis()) {
        context?.consentDataStore?.edit { preferences ->
            preferences[KEY_HAS_CONSENTED] = consented
            preferences[KEY_CONSENT_TIMESTAMP] = if (consented) timestamp else 0L
        }
    }

    open suspend fun revokeConsent() {
        context?.consentDataStore?.edit { preferences ->
            preferences[KEY_HAS_CONSENTED] = false
            preferences[KEY_CONSENT_TIMESTAMP] = 0L
        }
    }
}
