package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.core.common.runCatchingResult
import com.linkview.domain.repository.CardRepository

class DeleteCardUseCase(
    private val cardRepository: CardRepository
) {
    suspend fun getReferencingCount(cardId: String): Int {
        return cardRepository.getIncomingBacklinkCount(cardId)
    }

    suspend operator fun invoke(cardId: String): Result<Unit> {
        return runCatchingResult {
            cardRepository.deleteCard(cardId)
        }
    }
}
