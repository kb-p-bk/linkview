package com.linkview.domain.usecase

import com.linkview.core.model.Card
import com.linkview.domain.repository.CardRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutocompleteCardsUseCaseTest {

    private lateinit var cardRepository: CardRepository
    private lateinit var useCase: AutocompleteCardsUseCase

    @BeforeEach
    fun setUp() {
        cardRepository = mockk()
        useCase = AutocompleteCardsUseCase(cardRepository)
    }

    @Test
    fun `returns matching cards from repository`() = runTest {
        val candidates = listOf(
            Card(id = "1", title = "Gaius Marius"),
            Card(id = "2", title = "Gaius Gracchus")
        )
        coEvery { cardRepository.autocompleteCards("Gaius", "self-id", 10) } returns candidates

        val result = useCase("Gaius", excludeCardId = "self-id", limit = 10)
        assertEquals(2, result.size)
        assertEquals("Gaius Marius", result[0].title)
    }
}
