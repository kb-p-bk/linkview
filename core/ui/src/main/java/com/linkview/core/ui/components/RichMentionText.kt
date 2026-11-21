package com.linkview.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.linkview.core.common.mention.MentionTokenizer

@Composable
fun RichMentionText(
    rawText: String,
    onCardClick: (cardId: String) -> Unit,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val codeBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val linkColor = MaterialTheme.colorScheme.primary
    val linkBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)

    val tokens = MentionTokenizer.extractTokens(rawText)
    if (tokens.isEmpty()) {
        Text(
            text = parseMarkdownToAnnotatedString(rawText, codeBg, linkColor),
            style = style,
            modifier = modifier
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        var currentIndex = 0
        for (token in tokens) {
            val safeStart = token.startIndex.coerceIn(0, rawText.length)
            if (safeStart > currentIndex) {
                val plainSegment = rawText.substring(currentIndex, safeStart)
                if (plainSegment.isNotEmpty()) {
                    append(parseMarkdownToAnnotatedString(plainSegment, codeBg, linkColor))
                }
            }

            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "CARD_LINK_${token.targetCardId}",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = linkColor,
                        background = linkBg,
                        fontWeight = FontWeight.SemiBold
                    ),
                    focusedStyle = SpanStyle(
                        textDecoration = TextDecoration.Underline
                    )
                ),
                linkInteractionListener = { onCardClick(token.targetCardId) }
            )

            pushLink(linkAnnotation)
            append("@${token.label}")
            pop()

            currentIndex = token.endIndex.coerceIn(0, rawText.length)
        }

        if (currentIndex < rawText.length) {
            val trailing = rawText.substring(currentIndex)
            if (trailing.isNotEmpty()) {
                append(parseMarkdownToAnnotatedString(trailing, codeBg, linkColor))
            }
        }
    }

    Text(
        text = annotatedString,
        style = style,
        modifier = modifier
    )
}
