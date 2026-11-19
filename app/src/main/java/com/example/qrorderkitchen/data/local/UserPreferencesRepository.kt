package com.example.qrorderkitchen.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.qrorderkitchen.data.models.StaffModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "aurora_staff_session")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_STAFF_ID = stringPreferencesKey("staff_id")
        private val KEY_STAFF_NAME = stringPreferencesKey("staff_name")
        private val KEY_STAFF_USERNAME = stringPreferencesKey("staff_username")
        private val KEY_STAFF_ROLE = stringPreferencesKey("staff_role")
    }

    val staffSessionFlow: Flow<StaffModel?> = context.dataStore.data.map { prefs ->
        val isLoggedIn = prefs[KEY_IS_LOGGED_IN] ?: false
        if (!isLoggedIn) return@map null

        StaffModel(
            id = prefs[KEY_STAFF_ID] ?: "",
            name = prefs[KEY_STAFF_NAME] ?: "Nhân viên",
            username = prefs[KEY_STAFF_USERNAME] ?: "",
            role = prefs[KEY_STAFF_ROLE] ?: "waiter"
        )
    }

    suspend fun saveStaffSession(staff: StaffModel) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_STAFF_ID] = staff.id
            prefs[KEY_STAFF_NAME] = staff.name
            prefs[KEY_STAFF_USERNAME] = staff.username
            prefs[KEY_STAFF_ROLE] = staff.role
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
