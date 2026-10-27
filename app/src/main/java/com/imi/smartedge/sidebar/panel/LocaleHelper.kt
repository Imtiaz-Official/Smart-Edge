package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "side_panel_prefs"
    private const val KEY_LANGUAGE = "app_language"

    fun onAttach(context: Context): Context {
        val lang = getPersistedLanguage(context)
        return updateContextLocale(context, lang)
    }

    fun setLocale(context: Context, language: String): Context {
        persistLanguage(context, language)

        // 1. AndroidX / AppCompat official locale management (API 14-34)
        val appLocale = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)

        // 2. Direct configuration update for non-Activity context & background services
        return updateContextLocale(context, language)
    }

    fun getPersistedLanguage(context: Context): String {
        // First check our unified preferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (!saved.isNullOrEmpty()) return saved

        // Fallback to legacy panel_prefs key if present from earlier migration
        val legacy = context.getSharedPreferences("panel_prefs", Context.MODE_PRIVATE)
            .getString("Locale.Helper.Selected.Language", null)
        if (!legacy.isNullOrEmpty()) {
            persistLanguage(context, legacy)
            return legacy
        }

        // Fallback to AppCompatDelegate if set
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales.toLanguageTags()
            if (tag.isNotEmpty()) return tag.split(",")[0]
        }

        // Fallback to system locale if supported, else "en"
        val sysLang = Locale.getDefault().language
        return if (sysLang in listOf("en", "es", "uk")) sysLang else "en"
    }

    private fun persistLanguage(context: Context, language: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()
    }

    fun updateContextLocale(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)

        val res = context.resources
        val config = Configuration(res.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLayoutDirection(locale)
        }

        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        return context.createConfigurationContext(config)
    }
}
