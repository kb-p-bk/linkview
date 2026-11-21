package com.linkview.core.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `markdown web link parsing`() {
        val input = "Read more on [Wikipedia](https://en.wikipedia.org/wiki/Panaetius) for details"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("Read more on Wikipedia for details", result.text)
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertEquals(1, linkAnnotations.size)
        val urlAnnotation = linkAnnotations.first().item as androidx.compose.ui.text.LinkAnnotation.Url
        assertEquals("https://en.wikipedia.org/wiki/Panaetius", urlAnnotation.url)
    }

    @Test
    fun `autolink bare URL parsing`() {
        val input = "Source: https://en.wikipedia.org/wiki/Stoicism is helpful"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertEquals(1, linkAnnotations.size)
        val urlAnnotation = linkAnnotations.first().item as androidx.compose.ui.text.LinkAnnotation.Url
        assertEquals("https://en.wikipedia.org/wiki/Stoicism", urlAnnotation.url)
    }

    @Test
    fun `autolink bare URL with trailing punctuation`() {
        val input = "Check this link: https://example.com/page."
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertEquals(1, linkAnnotations.size)
        val urlAnnotation = linkAnnotations.first().item as androidx.compose.ui.text.LinkAnnotation.Url
        // Ensure trailing period is not part of the URL
        assertEquals("https://example.com/page", urlAnnotation.url)
    }

    @Test
    fun `code block containing URL is not converted to clickable link`() {
        val input = "Run `curl https://api.example.com` in terminal"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("Run curl https://api.example.com in terminal", result.text)
        val linkAnnotations = result.getLinkAnnotations(0, result.length)
        assertTrue(linkAnnotations.isEmpty())
    }

    @Test
    fun `text with asterisks or backticks that are unclosed`() {
        val input = "Note with * unclosed asterisk and ` backtick"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
    }

    @Test
    fun `hyphen bullet points are converted to bullet symbols`() {
        val input = "- First point\n- Second point"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("• First point\n• Second point", result.text)
    }

    @Test
    fun `asterisk bullet points are converted to bullet symbols`() {
        val input = "* First point\n* Second point"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("• First point\n• Second point", result.text)
    }

    @Test
    fun `indented bullet points preserve indentation`() {
        val input = "- Main point\n  - Sub point\n    - Deep sub point"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("• Main point\n  • Sub point\n    • Deep sub point", result.text)
    }

    @Test
    fun `bullet points with inline bold and italic formatting`() {
        val input = "- Item with **bold** and *italic*"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals("• Item with bold and italic", result.text)
    }

    @Test
    fun `hyphen in middle of text is not converted to bullet`() {
        val input = "This is 5 - 3 = 2 and a well-known hyphenated word"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
    }

    @Test
    fun `negative number or horizontal rule is not converted`() {
        val input = "-10 degrees and --- separator"
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
    }
}
