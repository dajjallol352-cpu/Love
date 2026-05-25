package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*

object LuaHighlighter {
    private val keywords = setOf(
        "local", "function", "end", "then", "if", "while", "do", "return", 
        "nil", "true", "false", "not", "and", "or", "else", "elseif", 
        "repeat", "until", "for", "in", "break"
    )

    private val globals = setOf(
        "game", "workspace", "script", "task", "print", "warn", "error", 
        "loadstring", "HttpGet", "Instance", "Players", "LocalPlayer", 
        "Character", "connect", "Connect", "spawn", "wait", "GetService"
    )

    fun highlight(text: String): AnnotatedString {
        return buildAnnotatedString {
            // Default plain style
            append(text)
            
            // 1. Highlight numbers
            val numberRegex = "\\b\\d+\\b".toRegex()
            numberRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = LuaNumber),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // 2. Highlight text characters and keywords
            val wordRegex = "[a-zA-Z_][a-zA-Z0-9_]*".toRegex()
            wordRegex.findAll(text).forEach { match ->
                val word = match.value
                when {
                    keywords.contains(word) -> {
                        addStyle(
                            style = SpanStyle(color = LuaKeyword, fontWeight = FontWeight.Bold),
                            start = match.range.first,
                            end = match.range.last + 1
                        )
                    }
                    globals.contains(word) -> {
                        addStyle(
                            style = SpanStyle(color = LuaGlobal),
                            start = match.range.first,
                            end = match.range.last + 1
                        )
                    }
                }
            }

            // 3. Highlight Strings (double quotes, single quotes and block strings [[ ]])
            val stringRegex = "(?:\".*?\")|(?:'.*?')|(?:\\[\\[.*?\\]\\])".toRegex(RegexOption.DOT_MATCHES_ALL)
            stringRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = LuaString),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // 4. Highlight Comments (single lines that start with -- or blocks --[[ ]])
            val commentRegex = "(--\\[\\[.*?\\)\\]\\])|(--.*)".toRegex()
            commentRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = LuaComment, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }
}
