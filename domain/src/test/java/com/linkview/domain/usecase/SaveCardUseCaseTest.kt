package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.core.model.Card
import com.linkview.core.model.ContextUnit
import com.linkview.domain.repository.CardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SaveCardUseCaseTest {

    private lateinit var cardRepository: CardRepository
    private lateinit var useCase: SaveCardUseCase

    @BeforeEach
    fun setUp() {
        cardRepository = mockk(relaxed = true)
        useCase = SaveCardUseCase(cardRepository)
    }

    @Test
    fun `fails when card title is blank`() = runTest {
        val card = Card(id = "1", title = "   ")
        val result = useCase(card, emptyList())

        assertTrue(result is Result.Error)
        assertEquals("Card title cannot be blank.", (result as Result.Error).message)
        coVerify(exactly = 0) { cardRepository.saveCard(any(), any()) }
    }

    @Test
    fun `succeeds and saves card when title is valid`() = runTest {
        val card = Card(id = "1", title = "Publius Rutilius Rufus")
        val units = listOf(
            ContextUnit(id = "u1", sourceCardId = "1", sortOrder = 0, rawText = "Note", plainText = "Note")
        )
        coEvery { cardRepository.saveCard(card, units) } returns Unit

        val result = useCase(card, units)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { cardRepository.saveCard(card, units) }
    }
}
