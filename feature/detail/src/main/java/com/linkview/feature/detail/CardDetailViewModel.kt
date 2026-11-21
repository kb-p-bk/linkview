package com.linkview.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.model.BackupCard
import com.linkview.core.model.Card
import com.linkview.core.model.ContextUnit
import com.linkview.domain.usecase.CheckDuplicateTitleUseCase
import com.linkview.domain.usecase.DeleteCardUseCase
import com.linkview.domain.usecase.GetCardDetailWithBacklinksUseCase
import com.linkview.domain.usecase.SaveCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val getCardDetailUseCase: GetCardDetailWithBacklinksUseCase,
    private val deleteCardUseCase: DeleteCardUseCase,
    private val saveCardUseCase: SaveCardUseCase,
    private val checkDuplicateTitleUseCase: CheckDuplicateTitleUseCase,
    private val cardRepository: com.linkview.domain.repository.CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

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

    suspend fun saveCardFromJson(targetCardId: String, jsonString: String): Result<Unit> {
        return try {
            val trimmed = jsonString.trim()
            if (trimmed.isBlank()) {
                return Result.Error(IllegalArgumentException("JSON cannot be blank."))
            }

            val backupCard = jsonParser.decodeFromString(BackupCard.serializer(), trimmed)
            val newTitle = backupCard.title.trim()
            if (newTitle.isBlank()) {
                return Result.Error(IllegalArgumentException("Card title cannot be blank."))
            }

            // Check duplicate title against other cards
            val duplicate = checkDuplicateTitleUseCase(newTitle, excludeCardId = targetCardId)
            if (duplicate != null) {
                return Result.Error(IllegalArgumentException("A card titled '$newTitle' already exists. Titles must be unique."))
            }

            // Get existing card to preserve timestamps or properties
            val existing = cardRepository.getCardById(targetCardId)
            val createdAt = existing?.createdAt ?: System.currentTimeMillis()
            val collectionId = existing?.collectionId ?: com.linkview.core.model.CardCollection.DEFAULT_ID
            val domainCard = Card(
                id = targetCardId,
                title = newTitle,
                tags = backupCard.allTags,
                type = backupCard.type?.trim()?.ifBlank { null } ?: backupCard.allTags.firstOrNull(),
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
                collectionId = collectionId
            )

            // Extract units: support both units (List<String>) and contextUnits (List<BackupUnit>)
            val rawUnitTexts: List<String> = when {
                backupCard.units.isNotEmpty() -> backupCard.units
                backupCard.contextUnits.isNotEmpty() -> backupCard.contextUnits.map { it.rawText }
                else -> emptyList()
            }

            val domainUnits = rawUnitTexts
                .filter { it.isNotBlank() }
                .mapIndexed { index, text ->
                    ContextUnit(
                        id = UUID.randomUUID().toString(),
                        sourceCardId = targetCardId,
                        sortOrder = index,
                        rawText = text,
                        plainText = MentionTokenizer.toPlainText(text)
                    )
                }

            val result = saveCardUseCase(domainCard, domainUnits)
            if (result is Result.Success) {
                loadCard(targetCardId)
            }
            result
        } catch (e: Exception) {
            Result.Error(e)
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
