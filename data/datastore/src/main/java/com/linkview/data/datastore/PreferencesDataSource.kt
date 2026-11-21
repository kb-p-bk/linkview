package com.linkview.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.linkview.core.model.SearchScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "linkview_settings")

@Singleton
class PreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keySearchScope = stringPreferencesKey("search_scope")

    val searchScope: Flow<SearchScope> = context.dataStore.data.map { preferences ->
        val scopeName = preferences[keySearchScope] ?: SearchScope.TITLE_AND_CONTEXT.name
        try {
            SearchScope.valueOf(scopeName)
        } catch (_: IllegalArgumentException) {
            SearchScope.TITLE_AND_CONTEXT
        }
    }

    suspend fun setSearchScope(scope: SearchScope) {
        context.dataStore.edit { preferences ->
            preferences[keySearchScope] = scope.name
        }
    }
}
