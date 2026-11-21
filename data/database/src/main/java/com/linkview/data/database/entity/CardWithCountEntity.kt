package com.linkview.data.database.entity

import androidx.room.Embedded
import com.linkview.core.model.Card

data class CardWithCountEntity(
    @Embedded val card: CardEntity,
    val unitCount: Int = 0
) {
    fun toDomain(): Card = card.toDomain().copy(unitCount = unitCount)
}
