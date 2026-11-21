package com.linkview.feature.cards

import com.linkview.core.model.Card
import com.linkview.core.model.CardCollection
import com.linkview.core.model.CardSortOrder
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.domain.usecase.SearchCardsUseCase

data class CardsUiState(
    val searchQuery: String = "",
    val activeScope: SearchScope = SearchScope.TITLE_AND_CONTEXT,
    val selectedTags: Set<String> = emptySet(),
    val allAvailableTags: List<String> = emptyList(),
    val tagSearchQuery: String = "",
    val allCards: List<Card> = emptyList(),
    val searchResults: List<SearchResultItem> = emptyList(),
    val selectedCardId: String? = null,
    val isLoading: Boolean = false,
    val displayLimit: Int = 1000,
    val collections: List<CardCollection> = emptyList(),
    val activeCollectionId: String = CardCollection.DEFAULT_ID,
    val hideStubs: Boolean = true,
    val sortOrder: CardSortOrder = CardSortOrder.RECENTLY_UPDATED
) {
    val activeCollectionName: String
        get() = collections.firstOrNull { it.id == activeCollectionId }?.name ?: CardCollection.DEFAULT_NAME

    val isSearching: Boolean get() = searchQuery.isNotBlank()

    val cleanSearchQuery: String
        get() = SearchCardsUseCase.parseQuery(searchQuery).first

    val parsedScope: SearchScope?
        get() = SearchCardsUseCase.parseQuery(searchQuery).second

    val effectiveScope: SearchScope
        get() = parsedScope ?: activeScope

    /**
     * Tags filtered by the tag search bar in the header
     */
    val filteredAvailableTags: List<String>
        get() = if (tagSearchQuery.isBlank()) {
            allAvailableTags
        } else {
            allAvailableTags.filter { it.contains(tagSearchQuery, ignoreCase = true) }
        }

    /**
     * Cards matching all selected tags, hide-stubs filter, and active sort order
     */
    val filteredCards: List<Card>
        get() {
            val base = if (hideStubs) allCards.filter { it.unitCount > 0 } else allCards
            val tagFiltered = if (selectedTags.isEmpty()) {
                base
            } else {
                base.filter { card ->
                    val cardTagsLower = card.allTags.map { it.lowercase() }
                    selectedTags.all { selectedTag -> cardTagsLower.contains(selectedTag.lowercase()) }
                }
            }
            return when (sortOrder) {
                CardSortOrder.RECENTLY_UPDATED -> tagFiltered.sortedByDescending { it.updatedAt }
                CardSortOrder.RECENTLY_CREATED -> tagFiltered.sortedByDescending { it.createdAt }
                CardSortOrder.ALPHABETICAL_ASC -> tagFiltered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                CardSortOrder.ALPHABETICAL_DESC -> tagFiltered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
                CardSortOrder.MOST_CONTEXT -> tagFiltered.sortedWith(
                    compareByDescending<Card> { it.unitCount }.thenByDescending { it.updatedAt }
                )
            }
        }

    val displayedCards: List<Card>
        get() = filteredCards.take(displayLimit)

    val hasMoreCards: Boolean
        get() = filteredCards.size > displayLimit

    val filteredSearchResults: List<SearchResultItem>
        get() {
            val base = if (hideStubs) searchResults.filter { it.authoredUnitCount > 0 } else searchResults
            val tagFiltered = if (selectedTags.isEmpty()) {
                base
            } else {
                base.filter { item ->
                    val cardTagsLower = item.card.allTags.map { it.lowercase() }
                    selectedTags.all { selectedTag -> cardTagsLower.contains(selectedTag.lowercase()) }
                }
            }
            return when (sortOrder) {
                CardSortOrder.RECENTLY_UPDATED -> tagFiltered.sortedByDescending { it.card.updatedAt }
                CardSortOrder.RECENTLY_CREATED -> tagFiltered.sortedByDescending { it.card.createdAt }
                CardSortOrder.ALPHABETICAL_ASC -> tagFiltered.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.card.title })
                CardSortOrder.ALPHABETICAL_DESC -> tagFiltered.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.card.title })
                CardSortOrder.MOST_CONTEXT -> tagFiltered.sortedWith(
                    compareByDescending<SearchResultItem> { it.authoredUnitCount }.thenByDescending { it.card.updatedAt }
                )
            }
        }

    val displayedSearchResults: List<SearchResultItem>
        get() = filteredSearchResults.take(displayLimit)

    val hasMoreSearchResults: Boolean
        get() = filteredSearchResults.size > displayLimit
}
