package com.linkview.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.model.SearchScope
import com.linkview.domain.repository.CardRepository
import com.linkview.domain.repository.PreferencesRepository
import com.linkview.domain.usecase.SearchCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DEFAULT_CARD_TYPES = listOf("Person", "Event", "Place", "Institution", "Concept")

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardsViewModel @Inject constructor(
    private val cardRepository: CardRepository,
    private val preferencesRepository: PreferencesRepository,
    private val searchCardsUseCase: SearchCardsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Observe all cards
        viewModelScope.launch {
            cardRepository.observeAllCards().collect { cards ->
                val allCardTags = cards.flatMap { it.allTags }
                _uiState.update { state ->
                    val combined = (state.allAvailableTags + allCardTags).distinct()
                    state.copy(allCards = cards, allAvailableTags = combined)
                }
            }
        }

        // Observe distinct tags (most used recently first)
        viewModelScope.launch {
            cardRepository.observeAllTags().collect { tags ->
                val allCardTags = _uiState.value.allCards.flatMap { it.allTags }
                val sortedTags = (tags + DEFAULT_CARD_TYPES + allCardTags).distinct()
                _uiState.update { it.copy(allAvailableTags = sortedTags) }
            }
        }

        // Observe search scope preference
        viewModelScope.launch {
            preferencesRepository.observeSearchScope().collect { scope ->
                _uiState.update { it.copy(activeScope = scope) }
            }
        }

        // Debounced search query (~150ms)
        searchQueryFlow
            .debounce(150)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    searchCardsUseCase(query, _uiState.value.activeScope)
                }
            }
            .onEach { results ->
                _uiState.update { it.copy(searchResults = results, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, isLoading = query.isNotBlank()) }
        searchQueryFlow.value = query
    }

    fun onScopeSelected(scope: SearchScope) {
        _uiState.update { it.copy(activeScope = scope) }
        viewModelScope.launch {
            preferencesRepository.setSearchScope(scope)
        }
        if (_uiState.value.isSearching) {
            searchQueryFlow.value = _uiState.value.searchQuery
        }
    }

    fun toggleTagFilter(tag: String) {
        _uiState.update { state ->
            val updated = state.selectedTags.toMutableSet()
            if (updated.contains(tag)) {
                updated.remove(tag)
            } else {
                updated.add(tag)
            }
            state.copy(selectedTags = updated)
        }
    }

    fun clearTagFilters() {
        _uiState.update { it.copy(selectedTags = emptySet(), tagSearchQuery = "") }
    }

    fun onTagSearchQueryChanged(query: String) {
        _uiState.update { it.copy(tagSearchQuery = query) }
    }

    fun resolveCardId(cardIdOrTitle: String): String {
        val cards = _uiState.value.allCards
        val matchingCard = cards.find { it.id == cardIdOrTitle }
            ?: cards.find { it.title.equals(cardIdOrTitle, ignoreCase = true) }
        return matchingCard?.id ?: cardIdOrTitle
    }

    fun addAndSelectCustomTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                selectedTags = state.selectedTags + trimmed,
                tagSearchQuery = ""
            )
        }
    }

    fun selectCard(cardId: String?) {
        _uiState.update { it.copy(selectedCardId = cardId) }
    }
}
