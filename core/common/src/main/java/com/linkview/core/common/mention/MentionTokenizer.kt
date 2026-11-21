package com.linkview.core.common.mention

/**
 * Tokenizer for LinkView @ mentions.
 * Canonical raw token syntax: @[Label](card:<uuid>)
 * Denormalized plain-text representation: @Label
 */
object MentionTokenizer {
    private val TOKEN_REGEX = Regex("""@\[([^\]]+)\]\(card:([a-zA-Z0-9\-_]+)\)""")

    /**
     * Extracts all unique referenced card IDs from the raw text.
     */
    fun extractReferences(rawText: String): Set<String> {
        return TOKEN_REGEX.findAll(rawText).map { it.groupValues[2] }.toSet()
    }

    /**
     * Extracts all mention tokens with label, target ID, and character indices.
     */
    fun extractTokens(rawText: String): List<MentionToken> {
        return TOKEN_REGEX.findAll(rawText).map { matchResult ->
            MentionToken(
                label = matchResult.groupValues[1],
                targetCardId = matchResult.groupValues[2],
                startIndex = matchResult.range.first,
                endIndex = matchResult.range.last + 1
            )
        }.toList()
    }

    /**
     * Converts raw text containing @[Label](card:uuid) into user-friendly plain text "@Label".
     */
    fun toPlainText(rawText: String): String {
        return TOKEN_REGEX.replace(rawText) { matchResult ->
            "@" + matchResult.groupValues[1]
        }
    }

    /**
     * Formats a label and target card ID into canonical raw token syntax.
     */
    fun formatToken(title: String, targetCardId: String): String {
        return "@[$title](card:$targetCardId)"
    }

    /**
     * Replaces the display label of existing tokens with updated titles using a lookup function.
     */
    fun updateMentionLabels(rawText: String, titleLookup: (cardId: String) -> String?): String {
        return TOKEN_REGEX.replace(rawText) { matchResult ->
            val cardId = matchResult.groupValues[2]
            val updatedTitle = titleLookup(cardId) ?: matchResult.groupValues[1]
            formatToken(updatedTitle, cardId)
        }
    }
}
