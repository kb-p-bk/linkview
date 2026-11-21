package com.linkview.core.model

data class CardWithContext(
    val card: Card,
    val authoredUnits: List<ContextUnit>,
    val incomingBacklinks: List<BacklinkItem>
)
