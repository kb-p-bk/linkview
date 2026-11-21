package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = 1,
    val exportedAt: String = "",
    val collections: List<CardCollection> = emptyList(),
    val cards: List<BackupCard> = emptyList()
)

@Serializable
data class ReadableCard(
    val title: String,
    val tags: List<String> = emptyList(),
    val units: List<String> = emptyList()
)

@Serializable
data class BackupCard(
    val id: String = "",
    val title: String,
    val tags: List<String> = emptyList(),
    val type: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val collectionId: String = CardCollection.DEFAULT_ID,
    val contextUnits: List<BackupUnit> = emptyList(),
    val units: List<String> = emptyList()
) {
    val allTags: List<String>
        get() = buildList {
            addAll(tags)
            if (!type.isNullOrBlank() && !contains(type)) {
                add(type)
            }
        }
}

@Serializable
data class BackupUnit(
    val id: String = "",
    val sortOrder: Int = 0,
    val rawText: String,
    val references: List<String> = emptyList()
)
