package com.linkview.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Parses inline Markdown formatting into an [AnnotatedString]:
 * - Inline code: `` `code` ``
 * - Bold + Italic: `***text***`
 * - Bold: `**text**`
 * - Italic: `*text*`
 * - Strikethrough: `~~text~~`
 */
fun parseMarkdownToAnnotatedString(
    markdown: String,
    codeBackground: Color = Color.Unspecified
): AnnotatedString {
    if (markdown.isEmpty()) return AnnotatedString("")

    return buildAnnotatedString {
        val regex = Regex("""(`([^`]+)`)|\*\*\*([^*]+)\*\*\*|\*\*([^*]+)\*\*|\*([^*]+)\*|~~([^~]+)~~""")
        var lastIndex = 0

        for (match in regex.findAll(markdown)) {
            val range = match.range
            if (range.first > lastIndex) {
                append(markdown.substring(lastIndex, range.first))
            }

            when {
                // `code` (group 2)
                match.groups[2] != null -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBackground
                        )
                    ) {
                        append(match.groups[2]!!.value)
                    }
                }
                // ***bold & italic*** (group 3)
                match.groups[3] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(match.groups[3]!!.value)
                    }
                }
                // **bold** (group 4)
                match.groups[4] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[4]!!.value)
                    }
                }
                // *italic* (group 5)
                match.groups[5] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groups[5]!!.value)
                    }
                }
                // ~~strike~~ (group 6)
                match.groups[6] != null -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(match.groups[6]!!.value)
                    }
                }
            }
            lastIndex = range.last + 1
        }

        if (lastIndex < markdown.length) {
            append(markdown.substring(lastIndex))
        }
    }
}
