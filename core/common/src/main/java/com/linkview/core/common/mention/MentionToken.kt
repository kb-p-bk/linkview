package com.linkview.core.common.mention

data class MentionToken(
    val label: String,
    val targetCardId: String,
    val startIndex: Int,
    val endIndex: Int
)
