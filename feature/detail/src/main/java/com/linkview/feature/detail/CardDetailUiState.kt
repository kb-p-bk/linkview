package com.linkview.feature.detail

import com.linkview.core.model.Card
import com.linkview.core.model.CardWithContext
import com.linkview.core.model.EditableUnit

data class CardDetailUiState(
    val cardId: String = "",
    val cardWithContext: CardWithContext? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConfirmingDelete: Boolean = false,
    val referencingBacklinkCount: Int = 0,
    val deleteSuccess: Boolean = false,
    val isEditingUnits: Boolean = false,
    val editableUnits: List<EditableUnit> = emptyList(),
    val activeMentionQuery: String? = null,
    val activeUnitIndex: Int? = null,
    val mentionStartIndex: Int = -1,
    val mentionEndIndex: Int = -1,
    val mentionSuggestions: List<Card> = emptyList(),
    val isSavingUnits: Boolean = false
)
