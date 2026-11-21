package com.linkview.feature.editor

import com.linkview.core.model.Card
import com.linkview.core.model.EditableUnit

data class CardEditorUiState(
    val cardId: String? = null,
    val title: String = "",
    val tags: List<String> = emptyList(),
    val allExistingTags: List<String> = emptyList(),
    val tagInputText: String = "",
    val units: List<EditableUnit> = listOf(EditableUnit()),
    val duplicateCard: Card? = null,
    val activeMentionQuery: String? = null,
    val activeUnitIndex: Int? = null,
    val mentionStartIndex: Int = -1,
    val mentionEndIndex: Int = -1,
    val mentionSuggestions: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
