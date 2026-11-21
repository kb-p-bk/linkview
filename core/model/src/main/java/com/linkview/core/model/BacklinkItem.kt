package com.linkview.core.model

data class BacklinkItem(
    val unitId: String,
    val sourceCardId: String,
    val sourceCardTitle: String,
    val rawText: String,
    val plainText: String,
    val sortOrder: Int
)
