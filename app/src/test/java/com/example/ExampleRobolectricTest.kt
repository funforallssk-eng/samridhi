package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.RichTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Samridhi", appName)
  }

  @Test
  fun `parse rich text formatting`() {
    val input = "Hello *world* and _italic_ with <u>underline</u>"
    val annotated = RichTextHelper.parseRichText(input)
    val stripped = RichTextHelper.stripFormatting(input)
    assertEquals("Hello world and italic with underline", stripped)
    assertTrue(annotated.text.contains("world"))
  }
}
