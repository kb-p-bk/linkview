package com.linkview.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.linkview.core.common.mention.MentionTokenizer

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RichMentionText(
    rawText: String,
    onCardClick: (cardId: String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val tokens = MentionTokenizer.extractTokens(rawText)
    if (tokens.isEmpty()) {
        Text(
            text = parseMarkdownToAnnotatedString(rawText, codeBg),
            style = style,
            modifier = modifier
        )
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        var currentIndex = 0
        for (token in tokens) {
            val safeStart = token.startIndex.coerceIn(0, rawText.length)
            if (safeStart > currentIndex) {
                val plainSegment = rawText.substring(currentIndex, safeStart)
                if (plainSegment.isNotEmpty()) {
                    Text(
                        text = parseMarkdownToAnnotatedString(plainSegment, codeBg),
                        style = style
                    )
                }
            }
            MentionChip(
                label = token.label,
                onClick = { onCardClick(token.targetCardId) }
            )
            currentIndex = token.endIndex.coerceIn(0, rawText.length)
        }

        if (currentIndex < rawText.length) {
            val trailing = rawText.substring(currentIndex)
            if (trailing.isNotEmpty()) {
                Text(
                    text = parseMarkdownToAnnotatedString(trailing, codeBg),
                    style = style
                )
            }
        }
    }
}
