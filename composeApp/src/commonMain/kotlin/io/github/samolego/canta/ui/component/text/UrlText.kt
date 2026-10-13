package io.github.samolego.canta.ui.component.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * Text that renders `http(s)://` URLs as clickable underlined links.
 *
 * Ported from the pre-KMP `UrlText` (commit e95526b, which itself replaced a
 * manual `ClickableText` + string-annotation implementation). Every API used
 * here is multiplatform, so links work on Android, desktop and web;
 * [LinkAnnotation.Url] clicks are opened through the ambient `LocalUriHandler`.
 *
 * Wrap in [androidx.compose.foundation.text.selection.SelectionContainer]
 * to get selectable text with working links, as done in `AppInfoDialog`.
 */
@Composable
fun UrlText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
) {
    val urlPattern = """(https?://[^\s<>"')\]]+)""".toRegex()
    val parts = text.split(urlPattern)
    val urls = urlPattern.findAll(text).map { it.value }.toList()

    Text(
        text = buildAnnotatedString {
            parts.forEachIndexed { index, part ->
                append(part)
                if (index < urls.size) {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        withLink(LinkAnnotation.Url(url = urls[index])) {
                            append(urls[index])
                        }
                    }
                }
            }
        },
        modifier = modifier,
        style = style,
    )
}
