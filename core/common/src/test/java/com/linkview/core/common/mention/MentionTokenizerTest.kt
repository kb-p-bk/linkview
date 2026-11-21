package com.linkview.core.common.mention

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MentionTokenizerTest {

    @Test
    fun `extractReferences finds single and multiple card IDs or titles`() {
        val raw = "Met with @[Gaius Marius](card:marius-1) and @[Lucius Sulla] in Rome."
        val refs = MentionTokenizer.extractReferences(raw)

        assertEquals(2, refs.size)
        assertTrue(refs.contains("marius-1"))
        assertTrue(refs.contains("Lucius Sulla"))
    }

    @Test
    fun `extractTokens extracts labels, ids and ranges correctly`() {
        val raw = "Served under @[Gaius Marius](card:marius-1)."
        val tokens = MentionTokenizer.extractTokens(raw)

        assertEquals(1, tokens.size)
        val token = tokens.first()
        assertEquals("Gaius Marius", token.label)
        assertEquals("marius-1", token.targetCardId)
        assertEquals(13, token.startIndex)
        assertEquals(43, token.endIndex)
        assertEquals("@[Gaius Marius](card:marius-1)", raw.substring(token.startIndex, token.endIndex))
    }

    @Test
    fun `extractTokens works with clean tokens without uuid`() {
        val raw = "Served under @[Gaius Marius]."
        val tokens = MentionTokenizer.extractTokens(raw)

        assertEquals(1, tokens.size)
        val token = tokens.first()
        assertEquals("Gaius Marius", token.label)
        assertEquals("Gaius Marius", token.targetCardId)
        assertEquals(13, token.startIndex)
        assertEquals(28, token.endIndex)
    }

    @Test
    fun `toPlainText formats tokens to at-label`() {
        val raw = "Served under @[Gaius Marius](card:marius-1) in @[Numidia]."
        val plain = MentionTokenizer.toPlainText(raw)

        assertEquals("Served under @Gaius Marius in @Numidia.", plain)
    }

    @Test
    fun `formatToken creates clean syntax without uuid`() {
        val token = MentionTokenizer.formatToken("Julius Caesar", "caesar-100")
        assertEquals("@[Julius Caesar]", token)
    }

    @Test
    fun `stripCardUuids removes uuid noise from text`() {
        val raw = "Met @[Gaius Marius](card:marius-1) and @[Sulla](card:sulla-2)."
        val stripped = MentionTokenizer.stripCardUuids(raw)
        assertEquals("Met @[Gaius Marius] and @[Sulla].", stripped)
    }

    @Test
    fun `updateMentionLabels updates token titles via lookup`() {
        val raw = "Served under @[Marius](card:marius-1) and @[Sulla]."
        val updated = MentionTokenizer.updateMentionLabels(raw) { idOrTitle ->
            when (idOrTitle) {
                "marius-1" -> "Gaius Marius"
                "Sulla" -> "Lucius Cornelius Sulla"
                else -> null
            }
        }
        assertEquals("Served under @[Gaius Marius] and @[Lucius Cornelius Sulla].", updated)
    }
}
