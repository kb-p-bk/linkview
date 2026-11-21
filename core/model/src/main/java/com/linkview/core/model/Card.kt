package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val type: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val collectionId: String = CardCollection.DEFAULT_ID,
    val unitCount: Int = 0
) {
    /**
     * Normalized list of all tags for this card, combining [tags] and legacy [type].
     */
    val allTags: List<String>
        get() = buildList {
            addAll(tags)
            if (!type.isNullOrBlank() && !contains(type)) {
                add(type)
            }
        }
}
