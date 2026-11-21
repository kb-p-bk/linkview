package com.linkview.core.model

import java.util.UUID

data class EditableUnit(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String = "",
    val cursorPosition: Int? = null
)
