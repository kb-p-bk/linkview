package com.linkview.data.repository

import com.linkview.core.common.DispatcherProvider
import com.linkview.core.model.SearchScope
import com.linkview.data.datastore.PreferencesDataSource
import com.linkview.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val preferencesDataSource: PreferencesDataSource,
    private val dispatchers: DispatcherProvider
) : PreferencesRepository {

    override fun observeSearchScope(): Flow<SearchScope> {
        return preferencesDataSource.searchScope.flowOn(dispatchers.io)
    }

    override suspend fun setSearchScope(scope: SearchScope) = withContext(dispatchers.io) {
        preferencesDataSource.setSearchScope(scope)
    }
}
