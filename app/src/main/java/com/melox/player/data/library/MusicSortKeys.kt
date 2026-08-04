package com.melox.player.data.library

import com.github.promeg.pinyinhelper.Pinyin
import java.util.Locale

internal data class MusicSortKeys(
    val section: String,
    val value: String,
)

internal fun createMusicSortKeys(text: String?): MusicSortKeys {
    val raw = text?.trim().orEmpty()
    if (raw.isEmpty()) return MusicSortKeys(section = "#", value = "2_")

    val firstCharacter = raw.first()
    if (firstCharacter.isDigit()) {
        return MusicSortKeys(section = "0", value = "0_$raw")
    }

    if (firstCharacter in 'A'..'Z' || firstCharacter in 'a'..'z') {
        val normalized = raw.uppercase(Locale.ROOT)
        return MusicSortKeys(
            section = normalized.first().toString(),
            value = "1_$normalized",
        )
    }

    val pinyin = try {
        Pinyin.toPinyin(raw, "").uppercase(Locale.ROOT)
    } catch (_: Exception) {
        ""
    }
    val pinyinInitial = pinyin.firstOrNull()
    if (pinyinInitial != null && pinyinInitial in 'A'..'Z') {
        return MusicSortKeys(
            section = pinyinInitial.toString(),
            value = "1_$pinyin",
        )
    }

    return MusicSortKeys(
        section = "#",
        value = "2_${raw.uppercase(Locale.ROOT)}",
    )
}
