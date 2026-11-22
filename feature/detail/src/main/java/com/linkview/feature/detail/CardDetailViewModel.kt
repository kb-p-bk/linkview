package com.linkview.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.model.BackupCard
import com.linkview.core.model.Card
import com.linkview.core.model.ContextUnit
import com.linkview.core.model.EditableUnit
import com.linkview.domain.usecase.AutocompleteCardsUseCase
import com.linkview.domain.usecase.CheckDuplicateTitleUseCase
import com.linkview.domain.usecase.DeleteCardUseCase
import com.linkview.domain.usecase.GetCardDetailWithBacklinksUseCase
import com.linkview.domain.usecase.SaveCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private data class MentionQueryData(
    val query: String,
    val unitIndex: Int,
    val startIndex: Int,
    val endIndex: Int
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class CardDetailViewModel @Inject constructor(
    private val getCardDetailUseCase: GetCardDetailWithBacklinksUseCase,
    private val deleteCardUseCase: DeleteCardUseCase,
    private val saveCardUseCase: SaveCardUseCase,
    private val checkDuplicateTitleUseCase: CheckDuplicateTitleUseCase,
    private val autocompleteCardsUseCase: AutocompleteCardsUseCase,
    private val cardRepository: com.linkview.domain.repository.CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private val mentionQueryFlow = MutableStateFlow<MentionQueryData?>(null)

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    init {
        mentionQueryFlow
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { queryData ->
                if (queryData == null) {
                    flowOf(emptyList())
                } else {
                    flow {
                        val suggestions = autocompleteCardsUseCase(
                            query = queryData.query,
                            excludeCardId = _uiState.value.cardId,
                            limit = 8
                        )
                        emit(suggestions)
                    }
                }
            }
            .onEach { suggestions ->
                _uiState.update { it.copy(mentionSuggestions = suggestions) }
            }
            .launchIn(viewModelScope)
    }

    companion object {
        private val contextScrollPositions = ConcurrentHashMap<String, Int>()
        private val extraContextScrollPositions = ConcurrentHashMap<String, Int>()
        private val selectedTabs = ConcurrentHashMap<String, String>()
    }

    fun getScrollPosition(cardId: String, tab: String = "CONTEXT"): Int =
        if (tab == "EXTRA_CONTEXT") extraContextScrollPositions[cardId] ?: 0
        else contextScrollPositions[cardId] ?: 0

    fun saveScrollPosition(cardId: String, tab: String, position: Int) {
        if (cardId.isNotBlank() && position >= 0) {
            if (tab == "EXTRA_CONTEXT") extraContextScrollPositions[cardId] = position
            else contextScrollPositions[cardId] = position
        }
    }

    fun getSelectedTab(cardId: String): String = selectedTabs[cardId] ?: "CONTEXT"

    fun saveSelectedTab(cardId: String, tab: String) {
        if (cardId.isNotBlank()) {
            selectedTabs[cardId] = tab
        }
    }

    fun loadCard(cardId: String) {
        if (cardId.isBlank()) return
        val isDifferentCard = _uiState.value.cardId != cardId
        val hasNoData = _uiState.value.cardWithContext == null
        _uiState.update {
            it.copy(
                cardId = cardId,
                isLoading = isDifferentCard && hasNoData,
                errorMessage = null,
                deleteSuccess = false,
                isConfirmingDelete = false,
                editingUnitIndex = if (isDifferentCard) null else it.editingUnitIndex,
                editableUnits = if (isDifferentCard) emptyList() else it.editableUnits,
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
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

    fun startEditingUnit(index: Int) {
        val currentUnits = _uiState.value.cardWithContext?.authoredUnits.orEmpty()
        if (index !in currentUnits.indices) return
        val editable = currentUnits.map {
            EditableUnit(id = it.id, rawText = MentionTokenizer.stripCardUuids(it.rawText))
        }
        _uiState.update {
            it.copy(
                editingUnitIndex = index,
                editableUnits = editable,
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun insertUnitBelow(index: Int) {
        val currentUnits = _uiState.value.cardWithContext?.authoredUnits.orEmpty()
        val editable = if (currentUnits.isNotEmpty()) {
            currentUnits.map {
                EditableUnit(id = it.id, rawText = MentionTokenizer.stripCardUuids(it.rawText))
            }.toMutableList()
        } else {
            mutableListOf()
        }

        val insertIndex = (index + 1).coerceIn(0, editable.size)
        editable.add(insertIndex, EditableUnit())

        _uiState.update {
            it.copy(
                editingUnitIndex = insertIndex,
                editableUnits = editable,
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun insertUnitAbove(index: Int) {
        val currentUnits = _uiState.value.cardWithContext?.authoredUnits.orEmpty()
        val editable = if (currentUnits.isNotEmpty()) {
            currentUnits.map {
                EditableUnit(id = it.id, rawText = MentionTokenizer.stripCardUuids(it.rawText))
            }.toMutableList()
        } else {
            mutableListOf()
        }

        val insertIndex = index.coerceIn(0, editable.size)
        editable.add(insertIndex, EditableUnit())

        _uiState.update {
            it.copy(
                editingUnitIndex = insertIndex,
                editableUnits = editable,
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun insertUnitAfterEditing(index: Int) {
        val current = _uiState.value.editableUnits.toMutableList()
        val insertIndex = (index + 1).coerceIn(0, current.size)
        current.add(insertIndex, EditableUnit())
        _uiState.update {
            it.copy(
                editableUnits = current,
                editingUnitIndex = insertIndex,
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun cancelEditingUnit() {
        mentionQueryFlow.value = null
        _uiState.update {
            it.copy(
                editingUnitIndex = null,
                editableUnits = emptyList(),
                activeMentionQuery = null,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun moveEditingUnit(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val updated = state.editableUnits.toMutableList()
            if (fromIndex in updated.indices && toIndex in updated.indices) {
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
            }
            state.copy(
                editableUnits = updated,
                editingUnitIndex = toIndex
            )
        }
    }

    fun deleteEditingUnit(index: Int) {
        val currentState = _uiState.value
        val cardWithContext = currentState.cardWithContext ?: return
        val card = cardWithContext.card
        val updated = currentState.editableUnits.toMutableList()
        if (index in updated.indices) {
            updated.removeAt(index)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingUnit = true) }
            val domainUnits = updated
                .filter { it.rawText.isNotBlank() }
                .mapIndexed { idx, unit ->
                    ContextUnit(
                        id = unit.id,
                        sourceCardId = card.id,
                        sortOrder = idx,
                        rawText = unit.rawText,
                        plainText = MentionTokenizer.toPlainText(unit.rawText)
                    )
                }

            when (val result = saveCardUseCase(card, domainUnits)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            editingUnitIndex = null,
                            isSavingUnit = false,
                            editableUnits = emptyList(),
                            activeMentionQuery = null,
                            mentionSuggestions = emptyList()
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingUnit = false,
                            errorMessage = result.message ?: "Failed to delete unit"
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onUnitTextChanged(index: Int, text: String, cursorPosition: Int) {
        _uiState.update { state ->
            val updated = state.editableUnits.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(rawText = text, cursorPosition = cursorPosition)
            }
            state.copy(editableUnits = updated)
        }

        // Detect @ mention query right before cursor
        val safeCursor = cursorPosition.coerceIn(0, text.length)
        val textBeforeCursor = text.substring(0, safeCursor)
        val atIndex = textBeforeCursor.lastIndexOf('@')

        if (atIndex != -1) {
            val queryCandidate = textBeforeCursor.substring(atIndex + 1)
            if (!queryCandidate.contains(Regex("[\\s\\[\\](),;!?:\"]"))) {
                var endIndex = safeCursor
                while (endIndex < text.length) {
                    val c = text[endIndex]
                    if (c.isWhitespace() || c == '[' || c == ']' || c == '(' || c == ')' ||
                        c == ',' || c == '.' || c == ';' || c == '!' || c == '?' || c == ':' || c == '"') {
                        break
                    }
                    endIndex++
                }

                _uiState.update {
                    it.copy(
                        activeMentionQuery = queryCandidate,
                        mentionStartIndex = atIndex,
                        mentionEndIndex = endIndex
                    )
                }
                mentionQueryFlow.value = MentionQueryData(
                    query = queryCandidate,
                    unitIndex = index,
                    startIndex = atIndex,
                    endIndex = endIndex
                )
                return
            }
        }

        mentionQueryFlow.value = null
        _uiState.update {
            it.copy(
                activeMentionQuery = null,
                mentionStartIndex = -1,
                mentionEndIndex = -1,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun selectMention(card: Card) {
        val state = _uiState.value
        val activeIndex = state.editingUnitIndex ?: return
        val currentUnits = state.editableUnits
        if (activeIndex !in currentUnits.indices) return

        val text = currentUnits[activeIndex].rawText
        val startIndex = state.mentionStartIndex
        val endIndex = state.mentionEndIndex

        val safeStart = if (startIndex in 0 until text.length && text[startIndex] == '@') {
            startIndex
        } else {
            text.lastIndexOf('@').takeIf { it != -1 } ?: return
        }

        val safeEnd = if (endIndex in (safeStart + 1)..text.length) {
            endIndex
        } else {
            var next = safeStart + 1
            while (next < text.length) {
                val c = text[next]
                if (c.isWhitespace() || c == '[' || c == ']' || c == '(' || c == ')' ||
                    c == ',' || c == '.' || c == ';' || c == '!' || c == '?' || c == ':' || c == '"') {
                    break
                }
                next++
            }
            next
        }

        val prefix = text.substring(0, safeStart)
        var suffix = text.substring(safeEnd)

        val tokenHasTrailingSpace = !suffix.startsWith(",") && !suffix.startsWith(".") && 
            !suffix.startsWith(";") && !suffix.startsWith("!") && !suffix.startsWith("?")
        
        if (suffix.startsWith(" ")) {
            suffix = suffix.substring(1)
        }

        val token = if (tokenHasTrailingSpace) {
            MentionTokenizer.formatToken(card.title, card.id) + " "
        } else {
            MentionTokenizer.formatToken(card.title, card.id)
        }

        val newText = prefix + token + suffix
        val newCursorPosition = (prefix + token).length

        _uiState.update { current ->
            val updated = current.editableUnits.toMutableList()
            updated[activeIndex] = updated[activeIndex].copy(
                rawText = newText,
                cursorPosition = newCursorPosition
            )
            current.copy(
                editableUnits = updated,
                activeMentionQuery = null,
                mentionStartIndex = -1,
                mentionEndIndex = -1,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun quickCreateAndSelectMention(stubTitle: String) {
        val trimmed = stubTitle.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val currentCard = _uiState.value.cardWithContext?.card
            val collectionId = currentCard?.collectionId ?: com.linkview.core.model.CardCollection.DEFAULT_ID
            val stubId = UUID.randomUUID().toString()
            val stubCard = Card(
                id = stubId,
                title = trimmed,
                collectionId = collectionId,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            cardRepository.saveCard(stubCard, emptyList())
            selectMention(stubCard)
        }
    }

    fun saveEditingUnit() {
        val currentState = _uiState.value
        val cardWithContext = currentState.cardWithContext ?: return
        val card = cardWithContext.card

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingUnit = true) }
            val domainUnits = currentState.editableUnits
                .filter { it.rawText.isNotBlank() }
                .mapIndexed { idx, unit ->
                    ContextUnit(
                        id = unit.id,
                        sourceCardId = card.id,
                        sortOrder = idx,
                        rawText = unit.rawText,
                        plainText = MentionTokenizer.toPlainText(unit.rawText)
                    )
                }

            when (val result = saveCardUseCase(card, domainUnits)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            editingUnitIndex = null,
                            isSavingUnit = false,
                            editableUnits = emptyList(),
                            activeMentionQuery = null,
                            mentionSuggestions = emptyList()
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isSavingUnit = false,
                            errorMessage = result.message ?: "Failed to save unit"
                        )
                    }
                }
                is Result.Loading -> Unit
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
