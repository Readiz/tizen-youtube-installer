// SPDX-License-Identifier: GPL-3.0-only
package dev.readiz.wgtinstaller

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object AppLanguage {
    fun wrap(context: Context): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(Locale.ENGLISH))
        return context.createConfigurationContext(config)
    }
}
