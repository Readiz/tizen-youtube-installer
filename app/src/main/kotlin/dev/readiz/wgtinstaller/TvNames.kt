// SPDX-License-Identifier: GPL-3.0-only
package dev.readiz.wgtinstaller

import android.text.Html

/** Decode name entities for display without treating literal TV-name text as HTML. */
internal object TvNames {
    private val entity = Regex("&(?:[A-Za-z][A-Za-z0-9]*|#[0-9]+|#[xX][0-9a-fA-F]+);")
    fun display(raw: String): String = entity.replace(raw) { match ->
        if (match.value == "&apos;") "'"
        else Html.fromHtml(match.value, Html.FROM_HTML_MODE_LEGACY).toString()
    }
}
