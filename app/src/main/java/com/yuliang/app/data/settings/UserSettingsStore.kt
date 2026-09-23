package com.yuliang.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("user_settings")

class UserSettingsStore(private val context: Context) {
    private val reduceMotionKey = booleanPreferencesKey("reduce_motion")
    val reduceMotion: Flow<Boolean> = context.dataStore.data.map { it[reduceMotionKey] ?: false }
    suspend fun setReduceMotion(value: Boolean) { context.dataStore.edit { it[reduceMotionKey] = value } }
}
