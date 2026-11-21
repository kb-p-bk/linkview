package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CardCollection(
    val id: String = DEFAULT_ID,
    val name: String = DEFAULT_NAME,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_ID = "default"
        const val DEFAULT_NAME = "Default"
    }
}
