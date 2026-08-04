package com.melox.player.ui.locale

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class AppLanguage(val languageTag: String) {
    FOLLOW_SYSTEM(""),
    SIMPLIFIED_CHINESE("zh-CN"),
    ENGLISH("en"),
}

fun currentAppLanguage(context: Context): AppLanguage {
    val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java)
            .applicationLocales
            .takeUnless(LocaleList::isEmpty)
            ?.get(0)
            ?.language
    } else {
        AppCompatDelegate.getApplicationLocales()
            .takeUnless(LocaleListCompat::isEmpty)
            ?.get(0)
            ?.language
    }
    return when (language) {
        "zh" -> AppLanguage.SIMPLIFIED_CHINESE
        "en" -> AppLanguage.ENGLISH
        else -> AppLanguage.FOLLOW_SYSTEM
    }
}

fun setAppLanguage(context: Context, language: AppLanguage) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            when (language) {
                AppLanguage.FOLLOW_SYSTEM -> LocaleList.getEmptyLocaleList()
                else -> LocaleList.forLanguageTags(language.languageTag)
            }
    } else {
        AppCompatDelegate.setApplicationLocales(
            when (language) {
                AppLanguage.FOLLOW_SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                else -> LocaleListCompat.forLanguageTags(language.languageTag)
            },
        )
    }
}
