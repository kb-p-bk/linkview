package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.domain.repository.CardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeleteCardUseCaseTest {

    private lateinit var cardRepository: CardRepository
    private lateinit var useCase: DeleteCardUseCase

    @BeforeEach
    fun setUp() {
        cardRepository = mockk(relaxed = true)
        useCase = DeleteCardUseCase(cardRepository)
    }

    @Test
    fun `getReferencingCount queries incoming backlinks count`() = runTest {
        coEvery { cardRepository.getIncomingBacklinkCount("card-1") } returns 5
        val count = useCase.getReferencingCount("card-1")
        assertEquals(5, count)
    }

    @Test
    fun `invoke deletes card via repository`() = runTest {
        coEvery { cardRepository.deleteCard("card-1") } returns Unit
        val result = useCase("card-1")
        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { cardRepository.deleteCard("card-1") }
    }
}
