package com.linkview.domain.usecase

import com.linkview.core.model.Card
import com.linkview.domain.repository.CardRepository

class CheckDuplicateTitleUseCase(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(title: String, excludeCardId: String? = null): Card? {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return null
        val existing = cardRepository.getCardByTitle(trimmed) ?: return null
        return if (excludeCardId != null && existing.id == excludeCardId) {
            null
        } else {
            existing
        }
    }
}
