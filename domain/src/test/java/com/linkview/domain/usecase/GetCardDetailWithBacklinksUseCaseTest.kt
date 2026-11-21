package com.linkview.domain.usecase

import com.linkview.core.model.BacklinkItem
import com.linkview.core.model.Card
import com.linkview.core.model.CardWithContext
import com.linkview.core.model.ContextUnit
import com.linkview.domain.repository.CardRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetCardDetailWithBacklinksUseCaseTest {

    private lateinit var cardRepository: CardRepository
    private lateinit var useCase: GetCardDetailWithBacklinksUseCase

    @BeforeEach
    fun setUp() {
        cardRepository = mockk()
        useCase = GetCardDetailWithBacklinksUseCase(cardRepository)
    }

    @Test
    fun `observes card with authored units and incoming backlinks`() = runTest {
        val expected = CardWithContext(
            card = Card(id = "marius", title = "Gaius Marius"),
            authoredUnits = listOf(
                ContextUnit("u1", "marius", 0, "General", "General")
            ),
            incomingBacklinks = listOf(
                BacklinkItem("u2", "rufus", "Publius Rufus", "Served with @[Gaius Marius](card:marius)", "Served with @Gaius Marius", 0)
            )
        )
        every { cardRepository.observeCardWithContext("marius") } returns flowOf(expected)

        useCase("marius").collect { result ->
            assertNotNull(result)
            assertEquals("Gaius Marius", result?.card?.title)
            assertEquals(1, result?.authoredUnits?.size)
            assertEquals(1, result?.incomingBacklinks?.size)
            assertEquals("Publius Rufus", result?.incomingBacklinks?.first()?.sourceCardTitle)
        }
    }
}
