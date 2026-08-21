package com.ecomap.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

class SessionDataStore(private val store: DataStore<Preferences>) {
    private val uid = stringPreferencesKey("firebase_uid")
    private val role = stringPreferencesKey("role_code")
    private val onboarding = booleanPreferencesKey("onboarding_completed")
    val session = store.data.map { Session(it[uid], it[role], it[onboarding] ?: false) }
    suspend fun saveSession(firebaseUid: String, roleCode: String) = store.edit { it[uid] = firebaseUid; it[role] = roleCode }
    suspend fun clearSession() = store.edit { it.remove(uid); it.remove(role) }
    suspend fun completeOnboarding() = store.edit { it[onboarding] = true }
}
data class Session(val firebaseUid: String?, val roleCode: String?, val onboardingCompleted: Boolean)
