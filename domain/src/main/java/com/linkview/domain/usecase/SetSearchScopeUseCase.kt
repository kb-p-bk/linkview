package com.linkview.domain.usecase

import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.PreferencesRepository

class SetSearchScopeUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(scope: SearchScope) {
        preferencesRepository.setSearchScope(scope)
    }
}
