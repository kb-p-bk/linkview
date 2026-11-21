package com.linkview.core.common.mention

/**
 * Tokenizer for LinkView @ mentions.
 * Canonical raw token syntax: @[Label](card:<uuid>) or @[Label]
 * Denormalized plain-text representation: @Label
 */
object MentionTokenizer {
    private val TOKEN_REGEX = Regex("""@\[([^\]]+)\](?:\(card:([a-zA-Z0-9\-_]+)\))?""")

    /**
     * Extracts all unique referenced card IDs or titles from the raw text.
     */
    fun extractReferences(rawText: String): Set<String> {
        return TOKEN_REGEX.findAll(rawText).map { matchResult ->
            val uuid = matchResult.groups[2]?.value
            if (!uuid.isNullOrEmpty()) uuid else matchResult.groupValues[1]
        }.toSet()
    }

    /**
     * Extracts all mention tokens with label, target ID (or title if ID absent), and character indices.
     */
    fun extractTokens(rawText: String): List<MentionToken> {
        return TOKEN_REGEX.findAll(rawText).map { matchResult ->
            val uuid = matchResult.groups[2]?.value
            MentionToken(
                label = matchResult.groupValues[1],
                targetCardId = if (!uuid.isNullOrEmpty()) uuid else matchResult.groupValues[1],
                startIndex = matchResult.range.first,
                endIndex = matchResult.range.last + 1
            )
        }.toList()
    }

    /**
     * Converts raw text containing @[Label](card:uuid) or @[Label] into user-friendly plain text "@Label".
     */
    fun toPlainText(rawText: String): String {
        return TOKEN_REGEX.replace(rawText) { matchResult ->
            "@" + matchResult.groupValues[1]
        }
    }

    /**
     * Formats a label into clean mention syntax without UUID noise.
     */
    fun formatToken(title: String, targetCardId: String? = null): String {
        return "@[$title]"
    }

    /**
     * Removes UUID noise from any legacy @[Label](card:uuid) tokens, converting to @[Label].
     */
    fun stripCardUuids(rawText: String): String {
        return TOKEN_REGEX.replace(rawText) { matchResult ->
            "@[" + matchResult.groupValues[1] + "]"
        }
    }

    /**
     * Replaces the display label of existing tokens with updated titles using a lookup function.
     */
    fun updateMentionLabels(rawText: String, titleLookup: (cardIdOrTitle: String) -> String?): String {
        return TOKEN_REGEX.replace(rawText) { matchResult ->
            val cardIdOrTitle = matchResult.groups[2]?.value ?: matchResult.groupValues[1]
            val updatedTitle = titleLookup(cardIdOrTitle) ?: matchResult.groupValues[1]
            formatToken(updatedTitle)
        }
    }
}
