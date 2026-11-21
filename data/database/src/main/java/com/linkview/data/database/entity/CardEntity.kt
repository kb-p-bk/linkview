package com.linkview.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [Index(value = ["title"], name = "index_cards_title")]
)
data class CardEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val title: String,
    val type: String?,
    val createdAt: Long,
    val updatedAt: Long
)
