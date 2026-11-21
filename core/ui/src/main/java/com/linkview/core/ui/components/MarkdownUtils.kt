package com.linkview.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Converts markdown list markers (- and *) at the beginning of lines to bullet points (•),
 * preserving any leading indentation.
 */
fun formatBulletPoints(markdown: String): String {
    if (markdown.isEmpty()) return markdown
    val bulletRegex = Regex("""^(\s*)[-*]\s+(.*)$""")
    return markdown.lines().joinToString("\n") { line ->
        val match = bulletRegex.matchEntire(line)
        if (match != null) {
            val indent = match.groupValues[1]
            val rest = match.groupValues[2]
            "$indent• $rest"
        } else {
            line
        }
    }
}

/**
 * Parses inline Markdown formatting into an [AnnotatedString]:
 * - Bullet lists: `- item` or `* item` -> `• item`
 * - Inline code: `` `code` ``
 * - Markdown web links: `[Label](https://url)` -> clickable URL link
 * - Autolink bare URLs: `https://...` or `http://...` -> clickable URL link
 * - Bold + Italic: `***text***`
 * - Bold: `**text**`
 * - Italic: `*text*`
 * - Strikethrough: `~~text~~`
 */
fun parseMarkdownToAnnotatedString(
    markdown: String,
    codeBackground: Color = Color.Unspecified,
    linkColor: Color = Color(0xFF0066CC)
): AnnotatedString {
    if (markdown.isEmpty()) return AnnotatedString("")

    val formattedMarkdown = formatBulletPoints(markdown)

    return buildAnnotatedString {
        // Regex order of precedence:
        // 1. `code`
        // 2. [label](url)
        // 3. https?://... bare URL
        // 4. ***bold&italic***
        // 5. **bold**
        // 6. *italic*
        // 7. ~~strike~~
        val regex = Regex(
            """(`([^`]+)`)""" +
            """|\[([^\]]+)\]\((https?://[^\s)]+)\)""" +
            """|(https?://[^\s<>"{}|\\^`]+)""" +
            """|\*\*\*([^*]+)\*\*\*""" +
            """|\*\*([^*]+)\*\*""" +
            """|\*([^*]+)\*""" +
            """|~~([^~]+)~~"""
        )
        var lastIndex = 0

        for (match in regex.findAll(formattedMarkdown)) {
            val range = match.range
            if (range.first > lastIndex) {
                append(formattedMarkdown.substring(lastIndex, range.first))
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
                // [label](url) (group 3 = label, group 4 = url)
                match.groups[3] != null && match.groups[4] != null -> {
                    val label = match.groups[3]!!.value
                    val url = match.groups[4]!!.value
                    pushLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Medium
                                ),
                                focusedStyle = SpanStyle(
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        )
                    )
                    append(label)
                    pop()
                }
                // Bare URL: https?://... (group 5)
                match.groups[5] != null -> {
                    val rawUrl = match.groups[5]!!.value
                    // Separate trailing punctuation like .,;:!?) from the URL
                    val trailingPunctuationRegex = Regex("""[.,;:!?)]+$""")
                    val punctMatch = trailingPunctuationRegex.find(rawUrl)
                    val cleanUrl = if (punctMatch != null) rawUrl.substring(0, punctMatch.range.first) else rawUrl
                    val trailingPunct = if (punctMatch != null) punctMatch.value else ""

                    if (cleanUrl.isNotEmpty()) {
                        pushLink(
                            LinkAnnotation.Url(
                                url = cleanUrl,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = linkColor,
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    focusedStyle = SpanStyle(
                                        textDecoration = TextDecoration.Underline,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            )
                        )
                        append(cleanUrl)
                        pop()
                    }
                    if (trailingPunct.isNotEmpty()) {
                        append(trailingPunct)
                    }
                }
                // ***bold & italic*** (group 6)
                match.groups[6] != null -> {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
                        append(match.groups[6]!!.value)
                    }
                }
                // **bold** (group 7)
                match.groups[7] != null -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groups[7]!!.value)
                    }
                }
                // *italic* (group 8)
                match.groups[8] != null -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groups[8]!!.value)
                    }
                }
                // ~~strike~~ (group 9)
                match.groups[9] != null -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(match.groups[9]!!.value)
                    }
                }
            }
            lastIndex = range.last + 1
        }

        if (lastIndex < formattedMarkdown.length) {
            append(formattedMarkdown.substring(lastIndex))
        }
    }
}
