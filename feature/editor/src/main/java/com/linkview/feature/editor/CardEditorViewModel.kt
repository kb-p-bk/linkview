package com.linkview.feature.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.model.Card
import com.linkview.core.model.ContextUnit
import com.linkview.domain.repository.CardRepository
import com.linkview.domain.usecase.AutocompleteCardsUseCase
import com.linkview.domain.usecase.CheckDuplicateTitleUseCase
import com.linkview.domain.usecase.GetCardDetailWithBacklinksUseCase
import com.linkview.domain.usecase.SaveCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

private val DEFAULT_CARD_TYPES = listOf("Person", "Event", "Place", "Institution", "Concept")

@HiltViewModel
class CardEditorViewModel @Inject constructor(
    private val saveCardUseCase: SaveCardUseCase,
    private val getCardDetailUseCase: GetCardDetailWithBacklinksUseCase,
    private val checkDuplicateTitleUseCase: CheckDuplicateTitleUseCase,
    private val autocompleteCardsUseCase: AutocompleteCardsUseCase,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardEditorUiState())
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cardRepository.observeAllTags().collect { tags ->
                val sortedTags = (tags + DEFAULT_CARD_TYPES).distinct()
                _uiState.update { it.copy(allExistingTags = sortedTags) }
            }
        }
    }

    fun loadCard(cardId: String?) {
        if (cardId == null) {
            _uiState.value = CardEditorUiState()
            return
        }

        viewModelScope.launch {
            _uiState.value = CardEditorUiState(cardId = cardId, isLoading = true)
            val cardWithContext = getCardDetailUseCase.getOnce(cardId)
            if (cardWithContext != null) {
                val editableUnits = if (cardWithContext.authoredUnits.isNotEmpty()) {
                    cardWithContext.authoredUnits.map {
                        EditableUnit(id = it.id, rawText = it.rawText)
                    }
                } else {
                    listOf(EditableUnit())
                }
                _uiState.update {
                    it.copy(
                        cardId = cardWithContext.card.id,
                        title = cardWithContext.card.title,
                        tags = cardWithContext.card.allTags,
                        units = editableUnits,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Card not found") }
            }
        }
    }

    fun resetState() {
        _uiState.value = CardEditorUiState()
    }

    fun resetSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun onTitleChanged(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, errorMessage = null) }
        viewModelScope.launch {
            val duplicate = checkDuplicateTitleUseCase(newTitle, excludeCardId = _uiState.value.cardId)
            _uiState.update { it.copy(duplicateCard = duplicate) }
        }
    }

    fun onTagInputChanged(input: String) {
        _uiState.update { it.copy(tagInputText = input) }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        _uiState.update { state ->
            if (state.tags.any { it.equals(trimmed, ignoreCase = true) }) {
                state.copy(tagInputText = "")
            } else {
                state.copy(tags = state.tags + trimmed, tagInputText = "")
            }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { state ->
            state.copy(tags = state.tags.filterNot { it.equals(tag, ignoreCase = true) })
        }
    }

    fun addContextUnit() {
        _uiState.update { state ->
            state.copy(units = state.units + EditableUnit())
        }
    }

    fun removeContextUnit(index: Int) {
        _uiState.update { state ->
            val updated = state.units.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            if (updated.isEmpty()) {
                updated.add(EditableUnit())
            }
            state.copy(
                units = updated,
                activeMentionQuery = if (state.activeUnitIndex == index) null else state.activeMentionQuery,
                activeUnitIndex = if (state.activeUnitIndex == index) null else state.activeUnitIndex
            )
        }
    }

    fun moveContextUnit(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val updated = state.units.toMutableList()
            if (fromIndex in updated.indices && toIndex in updated.indices) {
                val item = updated.removeAt(fromIndex)
                updated.add(toIndex, item)
            }
            state.copy(units = updated)
        }
    }

    fun onUnitTextChanged(index: Int, text: String, cursorPosition: Int) {
        _uiState.update { state ->
            val updated = state.units.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(rawText = text, cursorPosition = cursorPosition)
            }
            state.copy(units = updated)
        }

        // Detect @ mention query right before cursor
        val safeCursor = cursorPosition.coerceIn(0, text.length)
        val textBeforeCursor = text.substring(0, safeCursor)
        val atIndex = textBeforeCursor.lastIndexOf('@')

        if (atIndex != -1) {
            val candidateFromAt = textBeforeCursor.substring(atIndex)
            // Query is invalid if it crosses newlines or closing/opening brackets
            val isInsideExistingToken = candidateFromAt.contains('\n') || 
                candidateFromAt.contains('[') || 
                candidateFromAt.contains(']')

            if (!isInsideExistingToken) {
                // Determine word boundary after atIndex + 1 in full text
                var wordEndIndex = atIndex + 1
                while (wordEndIndex < text.length) {
                    val c = text[wordEndIndex]
                    if (c.isWhitespace() || c == '[' || c == ']' || c == '(' || c == ')' ||
                        c == ',' || c == '.' || c == ';' || c == '!' || c == '?' || c == ':' || c == '"') {
                        break
                    }
                    wordEndIndex++
                }

                // Range covers from atIndex up to wordEndIndex (or cursor if cursor moved beyond)
                val actualEndIndex = maxOf(wordEndIndex, safeCursor)
                val queryCandidate = text.substring(atIndex + 1, actualEndIndex)

                if (queryCandidate.length < 50) {
                    _uiState.update {
                        it.copy(
                            activeMentionQuery = queryCandidate,
                            activeUnitIndex = index,
                            mentionStartIndex = atIndex,
                            mentionEndIndex = actualEndIndex
                        )
                    }
                    viewModelScope.launch {
                        val suggestions = autocompleteCardsUseCase(
                            query = queryCandidate,
                            excludeCardId = _uiState.value.cardId,
                            limit = 8
                        )
                        _uiState.update { it.copy(mentionSuggestions = suggestions) }
                    }
                    return
                }
            }
        }

        // Dismiss suggestions if not in a valid mention
        _uiState.update {
            it.copy(
                activeMentionQuery = null,
                activeUnitIndex = null,
                mentionStartIndex = -1,
                mentionEndIndex = -1,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun selectMention(card: Card) {
        val state = _uiState.value
        val activeIndex = state.activeUnitIndex ?: return
        val currentUnits = state.units
        if (activeIndex !in currentUnits.indices) return

        val text = currentUnits[activeIndex].rawText
        val startIndex = state.mentionStartIndex
        val endIndex = state.mentionEndIndex

        // Validate mention start
        val safeStart = if (startIndex in 0 until text.length && text[startIndex] == '@') {
            startIndex
        } else {
            text.lastIndexOf('@').takeIf { it != -1 } ?: return
        }

        // Validate mention end
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

        // If suffix starts with a space, strip one leading space so formatToken's trailing space doesn't double-space
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
            val updated = current.units.toMutableList()
            updated[activeIndex] = updated[activeIndex].copy(
                rawText = newText,
                cursorPosition = newCursorPosition
            )
            current.copy(
                units = updated,
                activeMentionQuery = null,
                activeUnitIndex = null,
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
            val stubId = UUID.randomUUID().toString()
            val stubCard = Card(
                id = stubId,
                title = trimmed,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            // Save stub card directly so it exists in Room
            cardRepository.saveCard(stubCard, emptyList())
            selectMention(stubCard)
        }
    }

    fun dismissMentionSuggestions() {
        _uiState.update {
            it.copy(
                activeMentionQuery = null,
                activeUnitIndex = null,
                mentionStartIndex = -1,
                mentionEndIndex = -1,
                mentionSuggestions = emptyList()
            )
        }
    }

    fun save() {
        val currentState = _uiState.value
        val trimmedTitle = currentState.title.trim()
        if (trimmedTitle.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Title cannot be blank") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val cardId = currentState.cardId ?: UUID.randomUUID().toString()
            val card = Card(
                id = cardId,
                title = trimmedTitle,
                tags = currentState.tags,
                type = currentState.tags.firstOrNull()
            )
            val units = currentState.units
                .filter { it.rawText.isNotBlank() }
                .mapIndexed { index, unit ->
                    ContextUnit(
                        id = unit.id,
                        sourceCardId = cardId,
                        sortOrder = index,
                        rawText = unit.rawText,
                        plainText = MentionTokenizer.toPlainText(unit.rawText)
                    )
                }

            when (val result = saveCardUseCase(card, units)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = result.message ?: "Failed to save card")
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }
}
