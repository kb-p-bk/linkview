package com.linkview.domain.usecase

import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.PreferencesRepository
import com.linkview.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class SearchCardsUseCase(
    private val searchRepository: SearchRepository,
    private val preferencesRepository: PreferencesRepository
) {
    companion object {
        fun parseQuery(raw: String): Pair<String, SearchScope?> {
            val trimmed = raw.trim()
            return when {
                trimmed.startsWith("title:", ignoreCase = true) -> {
                    trimmed.substring(6).trim() to SearchScope.TITLE_ONLY
                }
                trimmed.startsWith("t:", ignoreCase = true) -> {
                    trimmed.substring(2).trim() to SearchScope.TITLE_ONLY
                }
                trimmed.startsWith("context:", ignoreCase = true) -> {
                    trimmed.substring(8).trim() to SearchScope.CONTEXT_ONLY
                }
                trimmed.startsWith("c:", ignoreCase = true) -> {
                    trimmed.substring(2).trim() to SearchScope.CONTEXT_ONLY
                }
                else -> trimmed to null
            }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(query: String, explicitScope: SearchScope? = null): Flow<List<SearchResultItem>> {
        val (cleanQuery, prefixScope) = parseQuery(query)
        if (cleanQuery.isEmpty()) {
            return flowOf(emptyList())
        }
        val targetScope = prefixScope ?: explicitScope
        return if (targetScope != null) {
            searchRepository.search(cleanQuery, targetScope)
        } else {
            preferencesRepository.observeSearchScope().flatMapLatest { scope ->
                searchRepository.search(cleanQuery, scope)
            }
        }
    }
}
