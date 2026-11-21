package com.linkview.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.linkview.core.model.CardCollection

@Entity(tableName = "collections")
data class CollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): CardCollection = CardCollection(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    companion object {
        fun fromDomain(collection: CardCollection): CollectionEntity = CollectionEntity(
            id = collection.id,
            name = collection.name,
            createdAt = collection.createdAt,
            updatedAt = collection.updatedAt
        )
    }
}
