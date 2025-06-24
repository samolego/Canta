package io.github.samolego.canta.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView


class CustomTextSelectionCallback(
    private val context: Context,
    private val textView: TextView
) : ActionMode.Callback {

    companion object {
        private const val MENU_ITEM_TRANSLATE_DEEPL = 100
        private const val MENU_ITEM_SEARCH_GOOGLE = 101
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ITEM_TRANSLATE_DEEPL, 5, "Translate with DeepL")
            .setIcon(android.R.drawable.ic_menu_search) // Use a standard icon or your custom one

        menu.add(Menu.NONE, MENU_ITEM_SEARCH_GOOGLE, 6, "Search")
            .setIcon(android.R.drawable.ic_menu_search)

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        val selectedText = getSelectedText()

        when (item.itemId) {
            MENU_ITEM_TRANSLATE_DEEPL -> {
                openDeepLTranslation(selectedText)
                mode.finish()
                return true
            }
            MENU_ITEM_SEARCH_GOOGLE -> {
                searchOnGoogle(selectedText)
                mode.finish()
                return true
            }
        }

        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
    }

    private fun getSelectedText(): String {
        val selectionStart = textView.selectionStart
        val selectionEnd = textView.selectionEnd

        if (selectionStart != selectionEnd) {
            val min = selectionStart.coerceAtMost(selectionEnd)
            val max = selectionStart.coerceAtLeast(selectionEnd)
            if (min >= 0 && max <= textView.text.length) {
                return textView.text.substring(min, max)
            }
        }
        return ""
    }

    private fun openDeepLTranslation(text: String) {
        if (text.isEmpty()) return

        val encodedText = Uri.encode(text)

        val deepLAppIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage("com.deepl.mobiletranslator")
        }

        val packageManager = context.packageManager
        val deepLAvailable = deepLAppIntent.resolveActivity(packageManager) != null

        if (deepLAvailable) {
            context.startActivity(deepLAppIntent)
        } else {
            val deepLUrl = "https://www.deepl.com/translator#auto/en/$encodedText"
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLUrl))
            context.startActivity(webIntent)
        }
    }

    private fun searchOnGoogle(text: String) {
        if (text.isEmpty()) return

        val encodedText = Uri.encode(text)
        val searchUrl = "https://www.google.com/search?q=$encodedText"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
        context.startActivity(intent)
    }
}
