package com.sleepysoong.armydiet.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.sleepysoong.armydiet.ui.theme.ArmyColors

@Composable
fun HighlightedText(
    text: String,
    keywords: Set<String>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    highlightColor: Color = ArmyColors.Highlight,
    maxLines: Int = Int.MAX_VALUE
) {
    if (text.isBlank() || text == "메뉴 정보 없음") {
        Text(
            text = "-",
            modifier = modifier,
            style = style,
            color = color.copy(alpha = 0.5f)
        )
        return
    }

    val annotatedString = buildAnnotatedString {
        // Split around spaces and commas to isolate words from punctuation
        val tokens = text.split(Regex("(?<=[\\s,])|(?=[\\s,])"))
        
        tokens.forEach { token ->
            val hasKeyword = keywords.any { keyword -> token.contains(keyword) }
            
            if (hasKeyword && token.isNotBlank() && !token.matches(Regex("[\\s,]+"))) {
                pushStyle(
                    SpanStyle(
                        background = ArmyColors.Highlighter,
                        color = ArmyColors.HighlighterText,
                        fontWeight = FontWeight.Bold
                    )
                )
                append(token)
                pop()
            } else {
                append(token)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}
