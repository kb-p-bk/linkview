package com.linkview.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.linkview.core.model.Card
import com.linkview.core.model.CardCollection

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["title"], name = "index_cards_title"),
        Index(value = ["collectionId"], name = "index_cards_collectionId")
    ]
)
data class CardEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val title: String,
    val type: String?,
    val tagsCsv: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "default")
    val collectionId: String = CardCollection.DEFAULT_ID
) {
    fun toDomain(): Card {
        val parsedTags = if (tagsCsv.isBlank()) {
            emptyList()
        } else {
            tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
        val combined = buildList {
            addAll(parsedTags)
            if (!type.isNullOrBlank() && !contains(type)) {
                add(type)
            }
        }
        return Card(
            id = id,
            title = title,
            tags = combined,
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt,
            collectionId = collectionId
        )
    }

    companion object {
        fun fromDomain(card: Card): CardEntity {
            val normalizedTags = card.allTags
            return CardEntity(
                id = card.id,
                title = card.title,
                type = card.type ?: normalizedTags.firstOrNull(),
                tagsCsv = normalizedTags.joinToString(","),
                createdAt = card.createdAt,
                updatedAt = card.updatedAt,
                collectionId = card.collectionId.ifBlank { CardCollection.DEFAULT_ID }
            )
        }
    }
}
