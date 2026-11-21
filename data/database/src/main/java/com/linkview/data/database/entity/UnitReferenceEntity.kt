package com.linkview.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "unit_references",
    primaryKeys = ["unitId", "targetCardId"],
    foreignKeys = [
        ForeignKey(
            entity = ContextUnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CardEntity::class,
            parentColumns = ["id"],
            childColumns = ["targetCardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["targetCardId"], name = "index_unit_references_targetCardId"),
        Index(value = ["unitId"], name = "index_unit_references_unitId")
    ]
)
data class UnitReferenceEntity(
    val unitId: String,
    val targetCardId: String
)
