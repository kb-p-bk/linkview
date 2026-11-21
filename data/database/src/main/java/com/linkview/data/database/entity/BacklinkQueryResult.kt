package com.linkview.data.database.entity

data class BacklinkQueryResult(
    val unitId: String,
    val sourceCardId: String,
    val sourceCardTitle: String,
    val rawText: String,
    val plainText: String,
    val sortOrder: Int
)
