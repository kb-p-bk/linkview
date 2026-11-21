package com.linkview.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "card_tags",
    primaryKeys = ["cardId", "tag"],
    foreignKeys = [
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["tag"], name = "index_card_tags_tag"),
        Index(value = ["cardId"], name = "index_card_tags_cardId")
    ]
)
data class CardTagEntity(
    val cardId: String,
    val tag: String
)
