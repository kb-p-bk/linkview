package com.linkview.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "context_units",
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sourceCardId"], name = "index_context_units_sourceCardId")]
)
data class ContextUnitEntity(
    @PrimaryKey
    val id: String,
    val sourceCardId: String,
    val sortOrder: Int,
    val rawText: String,
    val plainText: String
)
