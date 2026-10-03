package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import java.util.regex.Pattern

object RichTextHelper {

    /**
     * Parses bold (*text* or <b>text</b>),
     * italics (_text_ or <i>text</i>),
     * and underline (<u>text</u> or ~text~)
     * into Jetpack Compose AnnotatedString.
     */
    fun parseRichText(input: String): AnnotatedString {
        if (input.isEmpty()) return AnnotatedString("")

        // Token-based parser for tags: <b>...</b>, <i>...</i>, <u>...</u>, *...*, _..._, ~...~
        // We normalize HTML tags and markdown syntax into a unified representation
        val normalized = input
            .replace("<b>", "⟦B⟧").replace("</b>", "⟦/B⟧")
            .replace("<i>", "⟦I⟧").replace("</i>", "⟦/I⟧")
            .replace("<u>", "⟦U⟧").replace("</u>", "⟦/U⟧")

        // Also convert markdown pairs if present (*bold*, _italic_, ~underline~)
        val converted = convertMarkdownToTokens(normalized)

        return buildAnnotatedString {
            var i = 0
            val boldStack = mutableListOf<Int>()
            val italicStack = mutableListOf<Int>()
            val underlineStack = mutableListOf<Int>()

            while (i < converted.length) {
                when {
                    converted.startsWith("⟦B⟧", i) -> {
                        boldStack.add(length)
                        i += 3
                    }
                    converted.startsWith("⟦/B⟧", i) -> {
                        if (boldStack.isNotEmpty()) {
                            val start = boldStack.removeAt(boldStack.size - 1)
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, length)
                        }
                        i += 4
                    }
                    converted.startsWith("⟦I⟧", i) -> {
                        italicStack.add(length)
                        i += 3
                    }
                    converted.startsWith("⟦/I⟧", i) -> {
                        if (italicStack.isNotEmpty()) {
                            val start = italicStack.removeAt(italicStack.size - 1)
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                        }
                        i += 4
                    }
                    converted.startsWith("⟦U⟧", i) -> {
                        underlineStack.add(length)
                        i += 3
                    }
                    converted.startsWith("⟦/U⟧", i) -> {
                        if (underlineStack.isNotEmpty()) {
                            val start = underlineStack.removeAt(underlineStack.size - 1)
                            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                        }
                        i += 4
                    }
                    else -> {
                        append(converted[i])
                        i++
                    }
                }
            }
        }
    }

    private fun convertMarkdownToTokens(text: String): String {
        var result = text
        // Bold: *text* (excluding ⟦ and ⟧)
        result = result.replace(Regex("""\*([^*\n⟦⟧]+)\*""")) {
            "⟦B⟧${it.groupValues[1]}⟦/B⟧"
        }
        // Italic: _text_
        result = result.replace(Regex("""_([^_*\n⟦⟧]+)_""")) {
            "⟦I⟧${it.groupValues[1]}⟦/I⟧"
        }
        // Underline: ~text~
        result = result.replace(Regex("""~([^~*\n⟦⟧]+)~""")) {
            "⟦U⟧${it.groupValues[1]}⟦/U⟧"
        }
        return result
    }

    /**
     * Strips all rich text tags for clean preview in chat lists.
     */
    fun stripFormatting(input: String): String {
        return input
            .replace(Regex("""<[^>]*>"""), "")
            .replace(Regex("""⟦[^⟧]*⟧"""), "")
            .replace(Regex("""[*_~]"""), "")
            .trim()
    }

    /**
     * Applies formatting (bold, italic, underline) to the current selection
     * or inserts tags at the cursor in a TextFieldValue.
     */
    fun applyFormat(
        textFieldValue: TextFieldValue,
        tagOpen: String,
        tagClose: String
    ): TextFieldValue {
        val text = textFieldValue.text
        val selection = textFieldValue.selection

        return if (selection.collapsed) {
            // No selection: insert open and close tags, put cursor in between
            val newText = text.substring(0, selection.start) + tagOpen + tagClose + text.substring(selection.end)
            val newCursor = selection.start + tagOpen.length
            TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newCursor)
            )
        } else {
            // Selected range: wrap selection
            val start = selection.min
            val end = selection.max
            val selectedText = text.substring(start, end)
            val newText = text.substring(0, start) + tagOpen + selectedText + tagClose + text.substring(end)
            val newEnd = end + tagOpen.length + tagClose.length
            TextFieldValue(
                text = newText,
                selection = androidx.compose.ui.text.TextRange(newEnd)
            )
        }
    }
}
