package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.core.common.runCatchingResult
import com.linkview.core.model.Card
import com.linkview.core.model.ContextUnit
import com.linkview.domain.repository.CardRepository

class SaveCardUseCase(
    private val cardRepository: CardRepository
) {
    suspend operator fun invoke(card: Card, units: List<ContextUnit>): Result<Unit> {
        return runCatchingResult {
            require(card.title.isNotBlank()) { "Card title cannot be blank." }
            cardRepository.saveCard(card, units)
        }
    }
}
