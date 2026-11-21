package com.linkview.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarkdownUtilsTest {

    @Test
    fun `plain text parsing returns same text`() {
        val input = "Hello world from a context unit"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
    }

    @Test
    fun `bold text parsing`() {
        val input = "This is **bold** text"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("This is bold text", result.text)
    }

    @Test
    fun `italic text parsing`() {
        val input = "This is *italic* text"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("This is italic text", result.text)
    }

    @Test
    fun `code text parsing`() {
        val input = "This is `code` text"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("This is code text", result.text)
    }

    @Test
    fun `strikethrough text parsing`() {
        val input = "This is ~~strike~~ text"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("This is strike text", result.text)
    }

    @Test
    fun `text with asterisks or backticks that are unclosed`() {
        val input = "Note with * unclosed asterisk and ` backtick"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
    }
}
