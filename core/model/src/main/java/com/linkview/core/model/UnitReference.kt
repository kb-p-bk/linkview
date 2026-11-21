package com.linkview.core.model

import kotlinx.serialization.Serializable

@Serializable
data class UnitReference(
    val unitId: String,
    val targetCardId: String
)
