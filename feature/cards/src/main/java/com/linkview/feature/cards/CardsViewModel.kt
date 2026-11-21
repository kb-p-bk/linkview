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
                _uiState.update { state ->
                    val defaultSelected = if (state.selectedCardId == null && cards.isNotEmpty()) {
                        cards.first().id
                    } else {
                        state.selectedCardId
                    }
                    state.copy(allCards = cards, selectedCardId = defaultSelected)
                }
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
        viewModelScope.launch {
            preferencesRepository.setSearchScope(scope)
            _uiState.update { it.copy(activeScope = scope) }
            if (_uiState.value.searchQuery.isNotBlank()) {
                searchCardsUseCase(_uiState.value.searchQuery, scope).collect { results ->
                    _uiState.update { it.copy(searchResults = results) }
                }
            }
        }
    }

    fun onTypeSelected(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun selectCard(cardId: String) {
        _uiState.update { it.copy(selectedCardId = cardId) }
    }
}
