package com.linkview.feature.cards

import com.linkview.core.model.Card
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope

data class CardsUiState(
    val searchQuery: String = "",
    val activeScope: SearchScope = SearchScope.TITLE_AND_CONTEXT,
    val selectedType: String? = null,
    val allCards: List<Card> = emptyList(),
    val searchResults: List<SearchResultItem> = emptyList(),
    val selectedCardId: String? = null,
    val isLoading: Boolean = false
) {
    val isSearching: Boolean get() = searchQuery.isNotBlank()

    val filteredCards: List<Card>
        get() = if (selectedType.isNullOrBlank()) {
            allCards
        } else {
            allCards.filter { it.type.equals(selectedType, ignoreCase = true) }
        }

    val filteredSearchResults: List<SearchResultItem>
        get() = if (selectedType.isNullOrBlank()) {
            searchResults
        } else {
            searchResults.filter { it.card.type.equals(selectedType, ignoreCase = true) }
        }
}
