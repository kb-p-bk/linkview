package com.linkview.domain.usecase

import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.PreferencesRepository
import com.linkview.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

class SearchCardsUseCase(
    private val searchRepository: SearchRepository,
    private val preferencesRepository: PreferencesRepository
) {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(query: String, explicitScope: SearchScope? = null): Flow<List<SearchResultItem>> {
        return if (explicitScope != null) {
            searchRepository.search(query, explicitScope)
        } else {
            preferencesRepository.observeSearchScope().flatMapLatest { scope ->
                searchRepository.search(query, scope)
            }
        }
    }
}
