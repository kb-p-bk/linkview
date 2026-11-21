package com.linkview.domain.usecase

import com.linkview.core.model.Card
import com.linkview.domain.repository.CardRepository

class AutocompleteCardsUseCase(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(query: String, excludeCardId: String? = null, limit: Int = 10): List<Card> {
        return cardRepository.autocompleteCards(query, excludeCardId, limit)
    }
}
