
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

@Composable
fun UrlText(text: String) {
    val urlPattern = """(https?://[^\s<>"')\]]+)""".toRegex()
    val parts = text.split(urlPattern)
    val urls = urlPattern.findAll(text).map { it.value }.toList()

    Text(buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            append(part)
            if (index < urls.size) {
                withStyle(
                    SpanStyle(
                        color =  MaterialTheme.colorScheme.onSurface,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    withLink(LinkAnnotation.Url(url = urls[index])) {
                        append(urls[index])
                    }
                }
            }
        }
    })
}
