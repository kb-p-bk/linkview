package com.linkview.domain.usecase

import com.linkview.core.model.Card
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.PreferencesRepository
import com.linkview.domain.repository.SearchRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SearchCardsUseCaseTest {

    private lateinit var searchRepository: SearchRepository
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var useCase: SearchCardsUseCase

    @BeforeEach
    fun setUp() {
        searchRepository = mockk()
        preferencesRepository = mockk()
        useCase = SearchCardsUseCase(searchRepository, preferencesRepository)
    }

    @Test
    fun `uses explicit scope when provided`() = runTest {
        val results = listOf(
            SearchResultItem(card = Card("1", "Gaius Marius"), matchingSnippets = emptyList())
        )
        every { searchRepository.search("Marius", SearchScope.TITLE_ONLY) } returns flowOf(results)

        useCase("Marius", explicitScope = SearchScope.TITLE_ONLY).collect { items ->
            assertEquals(1, items.size)
            assertEquals("Gaius Marius", items[0].card.title)
        }
    }

    @Test
    fun `uses observed scope when explicit scope is omitted`() = runTest {
        val results = listOf(
            SearchResultItem(card = Card("1", "Gaius Marius"), matchingSnippets = emptyList())
        )
        every { preferencesRepository.observeSearchScope() } returns flowOf(SearchScope.CONTEXT_ONLY)
        every { searchRepository.search("Marius", SearchScope.CONTEXT_ONLY) } returns flowOf(results)

        useCase("Marius").collect { items ->
            assertEquals(1, items.size)
            assertEquals("Gaius Marius", items[0].card.title)
        }
    }
}
