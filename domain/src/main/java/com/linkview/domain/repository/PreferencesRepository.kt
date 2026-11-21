package com.linkview.domain.repository

import com.linkview.core.model.SearchScope
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observeSearchScope(): Flow<SearchScope>
    suspend fun setSearchScope(scope: SearchScope)
}
