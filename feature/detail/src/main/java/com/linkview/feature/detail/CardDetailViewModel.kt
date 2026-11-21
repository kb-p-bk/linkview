package com.linkview.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.domain.usecase.DeleteCardUseCase
import com.linkview.domain.usecase.GetCardDetailWithBacklinksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val getCardDetailUseCase: GetCardDetailWithBacklinksUseCase,
    private val deleteCardUseCase: DeleteCardUseCase,
    private val cardRepository: com.linkview.domain.repository.CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    fun loadCard(cardId: String) {
        if (cardId.isBlank()) return
        _uiState.update {
            it.copy(
                cardId = cardId,
                isLoading = true,
                errorMessage = null,
                deleteSuccess = false,
                isConfirmingDelete = false
            )
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val resolvedId = cardRepository.getCardById(cardId)?.id
                ?: cardRepository.getCardByTitle(cardId)?.id
                ?: cardId

            getCardDetailUseCase(resolvedId).collect { cardWithContext ->
                _uiState.update {
                    it.copy(
                        cardId = resolvedId,
                        cardWithContext = cardWithContext,
                        isLoading = false,
                        errorMessage = if (cardWithContext == null) "Card not found" else null
                    )
                }
            }
        }
    }

    fun requestDelete() {
        val currentCardId = _uiState.value.cardId
        viewModelScope.launch {
            val count = deleteCardUseCase.getReferencingCount(currentCardId)
            _uiState.update {
                it.copy(
                    isConfirmingDelete = true,
                    referencingBacklinkCount = count
                )
            }
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(isConfirmingDelete = false) }
    }

    fun resetDeleteSuccess() {
        _uiState.update { it.copy(deleteSuccess = false) }
    }

    fun confirmDelete() {
        val currentCardId = _uiState.value.cardId
        viewModelScope.launch {
            when (val result = deleteCardUseCase(currentCardId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isConfirmingDelete = false, deleteSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isConfirmingDelete = false,
                            errorMessage = result.message ?: "Failed to delete card"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }
}
