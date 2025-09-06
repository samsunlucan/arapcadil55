package com.example.dilson.utils

object ArabicUtils {
    // Remove common Arabic diacritics (tashkeel)
    private val TASHKEEL_REGEX = "[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]".toRegex()

    // Normalize some letter forms (e.g. different Alef forms to bare Alef)
    private val NORMALIZE_MAP = mapOf(
        '\u0622' to '\u0627', // ALEF WITH MADDA to ALEF
        '\u0623' to '\u0627', // ALEF WITH HAMZA ABOVE to ALEF
        '\u0625' to '\u0627', // ALEF WITH HAMZA BELOW to ALEF
        '\u06CC' to '\u064A', // FARSI YEH to YEH
        '\u06A9' to '\u0643'  // KEHEH to KAF
    )

    fun removeDiacritics(text: String): String {
        return text.replace(TASHKEEL_REGEX, "")
    }

    fun normalizeLetters(text: String): String {
        if (text.isEmpty()) return text
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(NORMALIZE_MAP.getOrDefault(ch, ch))
        }
        return sb.toString()
    }

    fun normalize(text: String): String = normalizeLetters(removeDiacritics(text)).trim()
}

