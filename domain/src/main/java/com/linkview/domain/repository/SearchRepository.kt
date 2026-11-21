package com.linkview.domain.repository

import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun search(query: String, scope: SearchScope): Flow<List<SearchResultItem>>
}
