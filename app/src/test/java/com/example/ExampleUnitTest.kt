package com.example

import androidx.compose.ui.text.input.TextFieldValue
import com.example.ui.components.RichTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testRichTextHelper_bold() {
        val parsed = RichTextHelper.parseRichText("Hello *bold* world")
        assertEquals("Hello bold world", parsed.text)
    }

    @Test
    fun testRichTextHelper_italic() {
        val parsed = RichTextHelper.parseRichText("Hello _italic_ world")
        assertEquals("Hello italic world", parsed.text)
    }

    @Test
    fun testRichTextHelper_underline() {
        val parsed = RichTextHelper.parseRichText("Hello <u>underline</u> world")
        assertEquals("Hello underline world", parsed.text)
    }

    @Test
    fun testRichTextHelper_applyFormat() {
        val initial = TextFieldValue("Hello world", androidx.compose.ui.text.TextRange(6, 11))
        val formatted = RichTextHelper.applyFormat(initial, "*", "*")
        assertEquals("Hello *world*", formatted.text)
    }
}
