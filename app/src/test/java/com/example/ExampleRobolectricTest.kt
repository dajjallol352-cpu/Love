package com.example

import android.content.Context
import androidx.compose.ui.text.SpanStyle
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.LuaHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Delta Script Saver", appName)
  }

  @Test
  fun `verify lua keyword highlighter styling`() {
    val code = "local x = 10"
    val highlighted = LuaHighlighter.highlight(code)
    
    // Assert characters match
    assertEquals(code, highlighted.text)
    
    // Verify that the 'local' keyword gets highlighted as a span style
    val spanStyles = highlighted.spanStyles
    assertTrue("Harus ada highlight span", spanStyles.isNotEmpty())
  }
}
