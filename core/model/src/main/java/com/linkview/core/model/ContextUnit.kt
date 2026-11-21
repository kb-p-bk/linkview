package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ContextUnit(
    val id: String,
    val sourceCardId: String,
    val sortOrder: Int,
    val rawText: String,
    val plainText: String
)
