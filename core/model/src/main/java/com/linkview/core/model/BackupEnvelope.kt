package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val schemaVersion: Int = 1,
    val exportedAt: String,
    val cards: List<BackupCard>
)

@Serializable
data class BackupCard(
    val id: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val type: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val contextUnits: List<BackupUnit> = emptyList()
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
    val id: String,
    val sortOrder: Int,
    val rawText: String,
    val references: List<String> = emptyList()
)
