package com.linkview.feature.cards

import com.linkview.core.model.Card
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope

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
    val displayLimit: Int = 1000
) {
    val isSearching: Boolean get() = searchQuery.isNotBlank()

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
     * Cards matching all selected tags
     */
    val filteredCards: List<Card>
        get() = if (selectedTags.isEmpty()) {
            allCards
        } else {
            allCards.filter { card ->
                val cardTagsLower = card.allTags.map { it.lowercase() }
                selectedTags.all { selectedTag -> cardTagsLower.contains(selectedTag.lowercase()) }
            }
        }

    val displayedCards: List<Card>
        get() = filteredCards.take(displayLimit)

    val hasMoreCards: Boolean
        get() = filteredCards.size > displayLimit

    val filteredSearchResults: List<SearchResultItem>
        get() = if (selectedTags.isEmpty()) {
            searchResults
        } else {
            searchResults.filter { item ->
                val cardTagsLower = item.card.allTags.map { it.lowercase() }
                selectedTags.all { selectedTag -> cardTagsLower.contains(selectedTag.lowercase()) }
            }
        }

    val displayedSearchResults: List<SearchResultItem>
        get() = filteredSearchResults.take(displayLimit)

    val hasMoreSearchResults: Boolean
        get() = filteredSearchResults.size > displayLimit
}
