package com.linkview.feature.editor

import com.linkview.core.model.Card
import java.util.UUID

data class EditableUnit(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String = ""
)

data class CardEditorUiState(
    val cardId: String? = null,
    val title: String = "",
    val type: String = "",
    val units: List<EditableUnit> = listOf(EditableUnit()),
    val duplicateCard: Card? = null,
    val activeMentionQuery: String? = null,
    val activeUnitIndex: Int? = null,
    val mentionSuggestions: List<Card> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)
