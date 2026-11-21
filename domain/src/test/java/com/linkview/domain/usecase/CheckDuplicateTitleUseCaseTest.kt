package com.linkview.domain.usecase

import com.linkview.core.model.Card
import com.linkview.domain.repository.CardRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CheckDuplicateTitleUseCaseTest {

    private lateinit var cardRepository: CardRepository
    private lateinit var useCase: CheckDuplicateTitleUseCase

    @BeforeEach
    fun setUp() {
        cardRepository = mockk()
        useCase = CheckDuplicateTitleUseCase(cardRepository)
    }

    @Test
    fun `returns null if title is blank`() = runTest {
        val result = useCase("   ")
        assertNull(result)
    }

    @Test
    fun `returns existing card if exact title match exists`() = runTest {
        val existing = Card(id = "card-1", title = "Gaius Marius")
        coEvery { cardRepository.getCardByTitle("Gaius Marius") } returns existing

        val result = useCase("Gaius Marius")
        assertEquals(existing, result)
    }

    @Test
    fun `ignores match when excludeCardId matches the existing card`() = runTest {
        val existing = Card(id = "card-1", title = "Gaius Marius")
        coEvery { cardRepository.getCardByTitle("Gaius Marius") } returns existing

        val result = useCase("Gaius Marius", excludeCardId = "card-1")
        assertNull(result)
    }
}
