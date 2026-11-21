package com.linkview.core.model

data class SearchResultItem(
    val card: Card,
    val matchingSnippets: List<String> = emptyList(),
    val authoredUnitCount: Int = 0,
    val incomingBacklinkCount: Int = 0
)
