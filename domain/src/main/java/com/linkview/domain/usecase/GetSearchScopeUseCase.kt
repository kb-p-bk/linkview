package com.linkview.domain.usecase

import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetSearchScopeUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): Flow<SearchScope> = preferencesRepository.observeSearchScope()
}
