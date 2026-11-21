package com.linkview.feature.detail

import com.linkview.core.model.CardWithContext

data class CardDetailUiState(
    val cardId: String = "",
    val cardWithContext: CardWithContext? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isConfirmingDelete: Boolean = false,
    val referencingBacklinkCount: Int = 0,
    val deleteSuccess: Boolean = false
)
