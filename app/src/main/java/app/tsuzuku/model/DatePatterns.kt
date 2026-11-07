package app.tsuzuku.model

import java.util.Locale

/** "7 November, Saturday" written the way each language writes it. */
fun dayAndWeekdayPattern(locale: Locale): String = when (locale.language) {
    "ja" -> "M月d日 EEEE"
    "ko" -> "M월 d일 EEEE"
    "fr", "it" -> "EEEE d MMMM"
    "ru" -> "EEEE, d MMMM"
    "de" -> "d. MMMM, EEEE"
    "es", "pt" -> "EEEE, d 'de' MMMM"
    else -> "d MMMM, EEEE"
}
