package com.linkview.core.common.mention

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MentionTokenizerTest {

    @Test
    fun `extractReferences finds single and multiple card IDs`() {
        val raw = "Met with @[Gaius Marius](card:marius-1) and @[Lucius Sulla](card:sulla-2) in Rome."
        val refs = MentionTokenizer.extractReferences(raw)

        assertEquals(2, refs.size)
        assertTrue(refs.contains("marius-1"))
        assertTrue(refs.contains("sulla-2"))
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
    fun `toPlainText formats tokens to at-label`() {
        val raw = "Served under @[Gaius Marius](card:marius-1) in @[Numidia](card:numidia-3)."
        val plain = MentionTokenizer.toPlainText(raw)

        assertEquals("Served under @Gaius Marius in @Numidia.", plain)
    }

    @Test
    fun `formatToken creates canonical syntax`() {
        val token = MentionTokenizer.formatToken("Julius Caesar", "caesar-100")
        assertEquals("@[Julius Caesar](card:caesar-100)", token)
    }

    @Test
    fun `updateMentionLabels updates token titles via lookup`() {
        val raw = "Served under @[Marius](card:marius-1)."
        val updated = MentionTokenizer.updateMentionLabels(raw) { id ->
            if (id == "marius-1") "Gaius Marius" else null
        }
        assertEquals("Served under @[Gaius Marius](card:marius-1).", updated)
    }
}
