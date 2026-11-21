package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: String,
    val title: String,
    val type: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
