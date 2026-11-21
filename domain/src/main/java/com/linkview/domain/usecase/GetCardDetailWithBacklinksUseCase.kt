package com.linkview.domain.usecase

import com.linkview.core.model.CardWithContext
import com.linkview.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow

class GetCardDetailWithBacklinksUseCase(
    private val cardRepository: CardRepository
) {
    operator fun invoke(cardId: String): Flow<CardWithContext?> {
        return cardRepository.observeCardWithContext(cardId)
    }

    suspend fun getOnce(cardId: String): CardWithContext? {
        return cardRepository.getCardWithContext(cardId)
    }
}
